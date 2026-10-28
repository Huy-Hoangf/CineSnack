package com.cinesnack.service;

import com.cinesnack.account.repository.UserRepository;
import com.cinesnack.account.service.UserService;
import com.cinesnack.booking.repository.OrderRepository;
import com.cinesnack.booking.service.OrderService;
import com.cinesnack.catalog.repository.BookingCatalogRepository;
import com.cinesnack.invoice.service.InvoiceService;
import com.cinesnack.payment.repository.PaymentRepository;
import com.cinesnack.payment.service.PaymentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.booking.entity.Order;
import com.cinesnack.payment.entity.Payment;
import com.cinesnack.account.entity.User;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GuestOrderServiceTest {

    @Test
    void staffCanReadAndCashPayGuestOrderButEmailMatchingUserCannotReadIt() {
        var orders = mock(OrderRepository.class);
        var userRepository = mock(UserRepository.class);
        var users = new UserService(userRepository);
        var payments = mock(PaymentRepository.class);
        var invoices = mock(InvoiceService.class);
        var clock = Clock.fixed(
            Instant.parse("2026-10-15T03:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
        );
        var now = LocalDateTime.now(clock);
        var guestOrder = new Order(
            7,
            null,
            "DHGUEST",
            "guest@example.com",
            new BigDecimal("75000"),
            "CHO_THANH_TOAN",
            now.plusMinutes(15),
            now
        );
        when(orders.timTheoMa("DHGUEST")).thenReturn(guestOrder);
        when(orders.timVaKhoa("DHGUEST")).thenReturn(guestOrder);
        when(userRepository.timTheoThuDienTu("staff@example.com")).thenReturn(
            new User(2, "staff@example.com", "unused", "NHAN_VIEN", "HOAT_DONG")
        );
        when(userRepository.timTheoThuDienTu("guest@example.com")).thenReturn(
            new User(3, "guest@example.com", "unused", "KHACH_HANG", "HOAT_DONG")
        );
        var service = new OrderService(orders, mock(BookingCatalogRepository.class), users, clock);

        assertEquals(guestOrder, service.layChiTiet("DHGUEST", "staff@example.com").donHang());
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                service.layChiTiet("DHGUEST", "guest@example.com")
            )
                .getStatusCode()
                .value()
        );

        when(orders.coTheThanhToan(7, now)).thenReturn(true);
        when(orders.tinhTongChiTiet(7)).thenReturn(new BigDecimal("75000"));
        when(orders.doiTrangThai(7, "CHO_THANH_TOAN", "DA_THANH_TOAN")).thenReturn(1);
        var payment = new Payment(
            8,
            7,
            "TM01",
            "TIEN_MAT",
            new BigDecimal("75000"),
            "THANH_CONG",
            now
        );
        when(payments.taoThanhToanTienMat(eq(7L), any(), eq(now))).thenReturn(payment);
        var receipt = new PaymentService(orders, payments, invoices, users, clock).thanhToanTaiQuay(
            "DHGUEST",
            new BigDecimal("100000"),
            "staff@example.com"
        );
        assertEquals(payment, receipt.giaoDich());
        assertEquals(0, new BigDecimal("25000").compareTo(receipt.tienThua()));
        verify(invoices).phatHanh(guestOrder, now);
    }
}
