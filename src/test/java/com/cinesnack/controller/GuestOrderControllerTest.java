package com.cinesnack.controller;

import com.cinesnack.account.service.UserService;
import com.cinesnack.booking.controller.GuestOrderController;
import com.cinesnack.booking.service.OrderExpiryService;
import com.cinesnack.booking.service.OrderService;
import com.cinesnack.common.controller.ApiExceptionHandler;
import com.cinesnack.invoice.service.InvoiceService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cinesnack.invoice.entity.Invoice;
import com.cinesnack.booking.entity.Order;
import com.cinesnack.catalog.repository.BookingCatalogRepository;
import com.cinesnack.invoice.repository.InvoiceRepository;
import com.cinesnack.booking.repository.OrderRepository;
import com.cinesnack.booking.security.GuestOrderAccess;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GuestOrderControllerTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final BookingCatalogRepository catalog = mock(BookingCatalogRepository.class);
    private final UserService users = mock(UserService.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final GuestOrderAccess access = new GuestOrderAccess();
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-10-15T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final LocalDateTime now = LocalDateTime.now(clock);
    private MockMvc mvc;
    private final String request = """
    {"email":"guest@example.com","showTimeId":null,"seatIds":[],
     "snacks":[],"combos":[{"comboId":1,"quantity":2}]}
    """;

    @BeforeEach
    void setup() {
        var service = new OrderService(orders, catalog, users, clock);
        mvc = MockMvcBuilders.standaloneSetup(
            new GuestOrderController(
                service,
                new InvoiceService(invoices, service),
                mock(OrderExpiryService.class),
                access
            )
        )
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        when(catalog.timCombo(1)).thenReturn(
            new BookingCatalogRepository.Food(1, "Combo", new BigDecimal("59000"), "HOAT_DONG")
        );
        when(orders.taoDonHang(isNull(), anyString(), anyString(), any(), any(), any())).thenReturn(
            7L
        );
        when(orders.timTheoMa("DHGUEST")).thenReturn(order(null, "CHO_THANH_TOAN"));
        when(orders.timVaKhoa("DHGUEST")).thenReturn(order(null, "CHO_THANH_TOAN"));
        when(orders.layVe(7)).thenReturn(List.of());
        when(orders.layBapNuoc(7)).thenReturn(List.of());
    }

    @Test
    void guestCreatesOrderAndOwnsItOnlyInItsSession() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(
            post("/api/guest/orders")
                .session(session)
                .contentType("application/json")
                .content(request)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.total").value(118000))
            .andExpect(jsonPath("$.email").value("guest@example.com"));
        assertEquals(1, access.layMaDon(session).size());
        verify(orders).taoDonHang(
            isNull(),
            anyString(),
            eq("guest@example.com"),
            eq(new BigDecimal("118000")),
            eq(now.plusMinutes(15)),
            eq(now)
        );
        verifyNoInteractions(users);
    }

    @Test
    void guestEmailIsMandatoryAndValidatedBeforeCreatingOrder() throws Exception {
        for (String email : List.of("", "not-an-email")) {
            mvc.perform(
                post("/api/guest/orders")
                    .contentType("application/json")
                    .content(request.replace("guest@example.com", email))
            ).andExpect(status().isBadRequest());
        }
        verify(orders, never()).taoDonHang(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void guestTicketUsesCatalogPriceAndExistingSeatLockRules() throws Exception {
        when(catalog.timVaKhoaSuatChieu(3)).thenReturn(
            new BookingCatalogRepository.Show(
                3,
                2,
                new BigDecimal("75000"),
                now.plusHours(1),
                "SAP_CHIEU"
            )
        );
        when(catalog.timGhe(4)).thenReturn(new BookingCatalogRepository.Seat(4, 2, "HOAT_DONG"));
        String withTicket = request.replace(
            "\"showTimeId\":null,\"seatIds\":[]",
            "\"showTimeId\":3,\"seatIds\":[4]"
        );
        mvc.perform(
            post("/api/guest/orders")
                .session(new MockHttpSession())
                .contentType("application/json")
                .content(withTicket)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(193000));
        verify(orders).themVe(7, 3, 4, new BigDecimal("75000"));
        when(orders.gheDaDuocDat(3, 4)).thenReturn(true);
        mvc.perform(
            post("/api/guest/orders")
                .session(new MockHttpSession())
                .contentType("application/json")
                .content(withTicket)
        ).andExpect(status().isConflict());
    }

    @Test
    void anotherSessionCannotReadCancelOrSeeInvoicesEvenWithSameEmail() throws Exception {
        var owner = ownedSession();
        mvc.perform(get("/api/guest/orders/DHGUEST").session(owner)).andExpect(status().isOk());
        var stranger = new MockHttpSession();
        mvc.perform(
            get("/api/guest/orders/DHGUEST").session(stranger).param("email", "guest@example.com")
        ).andExpect(status().isForbidden());
        mvc.perform(post("/api/guest/orders/DHGUEST/cancel").session(stranger)).andExpect(
            status().isForbidden()
        );
        mvc.perform(get("/api/guest/orders/DHGUEST/invoices").session(stranger)).andExpect(
            status().isForbidden()
        );
        verify(orders, never()).timVaKhoa(anyString());
        verifyNoInteractions(invoices);
    }

    @Test
    void guestListCannotExposeAnotherGuestOrUserOrder() throws Exception {
        var owner = ownedSession();
        when(orders.layDanhSachKhach(anySet(), eq(0), eq(20))).thenAnswer(invocation ->
            invocation.<java.util.Set<String>>getArgument(0).contains("DHGUEST")
                ? List.of(order(null, "CHO_THANH_TOAN"))
                : List.of()
        );
        mvc.perform(get("/api/guest/orders").session(owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("DHGUEST"));
        mvc.perform(get("/api/guest/orders").session(new MockHttpSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        when(orders.timTheoMa("DHGUEST")).thenReturn(order(12L, "CHO_THANH_TOAN"));
        mvc.perform(get("/api/guest/orders/DHGUEST").session(owner)).andExpect(
            status().isForbidden()
        );
        mvc.perform(get("/api/guest/orders/DHGUEST/invoices").session(owner)).andExpect(
            status().isForbidden()
        );
        when(orders.timVaKhoa("DHGUEST")).thenReturn(order(12L, "CHO_THANH_TOAN"));
        mvc.perform(post("/api/guest/orders/DHGUEST/cancel").session(owner)).andExpect(
            status().isForbidden()
        );
    }

    @Test
    void ownerCanCancelPendingButNotPaidOrder() throws Exception {
        var owner = ownedSession();
        when(orders.doiTrangThai(7, "CHO_THANH_TOAN", "HUY")).thenReturn(1);
        mvc.perform(post("/api/guest/orders/DHGUEST/cancel").session(owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("HUY"));
        verify(orders).dongChiTiet(7, "HUY");
        when(orders.timVaKhoa("DHGUEST")).thenReturn(order(null, "DA_THANH_TOAN"));
        mvc.perform(post("/api/guest/orders/DHGUEST/cancel").session(owner)).andExpect(
            status().isConflict()
        );
    }

    @Test
    void ownerCanReadIssuedInvoiceAfterStaffPayment() throws Exception {
        when(orders.timTheoMa("DHGUEST")).thenReturn(order(null, "DA_THANH_TOAN"));
        when(invoices.timTheoDonHang(7)).thenReturn(
            List.of(
                new Invoice(
                    8,
                    7,
                    "HDB01",
                    "BONG_NUOC",
                    "guest@example.com",
                    new BigDecimal("118000"),
                    now,
                    List.of()
                )
            )
        );
        mvc.perform(get("/api/guest/orders/DHGUEST/invoices").session(ownedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("HDB01"));
    }

    private MockHttpSession ownedSession() {
        var session = new MockHttpSession();
        access.ghiNhanDonDaTao(session, order(null, "CHO_THANH_TOAN"));
        return session;
    }

    private Order order(Long userId, String status) {
        return new Order(
            7,
            userId,
            "DHGUEST",
            "guest@example.com",
            new BigDecimal("118000"),
            status,
            now.plusMinutes(15),
            now
        );
    }
}
