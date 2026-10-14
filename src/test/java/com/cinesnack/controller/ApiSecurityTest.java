package com.cinesnack.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cinesnack.config.ApiSecurityConfig;
import com.cinesnack.entity.User;
import com.cinesnack.repository.UserRepository;
import com.cinesnack.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(ApiSecurityTest.Config.class)
@WebAppConfiguration
class ApiSecurityTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
        ApiSecurityConfig.class,
        OrderController.class,
        PaymentController.class,
        InvoiceController.class,
        CsrfController.class,
        CatalogController.class,
        ApiExceptionHandler.class,
    })
    static class Config {

        @Bean
        UserRepository khoNguoiDung() {
            return mock(UserRepository.class);
        }

        @Bean
        OrderService dichVuDonHang() {
            return mock(OrderService.class);
        }

        @Bean
        PaymentService dichVuThanhToan() {
            return mock(PaymentService.class);
        }

        @Bean
        InvoiceService dichVuHoaDon() {
            return mock(InvoiceService.class);
        }

        @Bean
        CatalogService danhMuc() {
            return mock(CatalogService.class);
        }

        @Bean
        OrderExpiryService dichVuDonHetHan() {
            return mock(OrderExpiryService.class);
        }

        @Bean
        static org.springframework.validation.beanvalidation.MethodValidationPostProcessor kiemTraThamSoHam() {
            return new org.springframework.validation.beanvalidation.MethodValidationPostProcessor();
        }
    }

    @Autowired
    WebApplicationContext nguCanh;

    @Autowired
    PaymentService dichVuThanhToan;

    @Autowired
    OrderService dichVuDonHang;

    @Autowired
    UserRepository khoNguoiDung;

    @Autowired
    PasswordEncoder boMaHoa;

    private MockMvc kiemThuHttp;

    @BeforeEach
    void chuanBi() {
        reset(dichVuThanhToan, dichVuDonHang, khoNguoiDung);
        kiemThuHttp = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
            nguCanh
        )
            .apply(springSecurity())
            .build();
    }

    @Test
    void guestDuocXemDanhMucNhungThamSoSaiBiChan() throws Exception {
        kiemThuHttp
            .perform(get("/api/movies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
        kiemThuHttp.perform(get("/api/movies?size=101")).andExpect(status().isBadRequest());
        kiemThuHttp.perform(get("/api/showtimes?movieId=0")).andExpect(status().isBadRequest());
        kiemThuHttp
            .perform(get("/api/admin/orders").with(user("customer").roles("KHACH_HANG")))
            .andExpect(status().isForbidden());
    }

    @Test
    void guestDuocLayPhimTheoMa() throws Exception {
        kiemThuHttp.perform(get("/api/movies/1")).andExpect(status().isOk());
        kiemThuHttp.perform(get("/api/movies/0")).andExpect(status().isBadRequest());
    }

    @Test
    void chuaDangNhapKhongDuocXemDon() throws Exception {
        kiemThuHttp.perform(get("/api/orders/DH01")).andExpect(status().isUnauthorized());
        verifyNoInteractions(dichVuDonHang);
    }

    @Test
    void khachKhongThuTienDuCoCsrf() throws Exception {
        kiemThuHttp
            .perform(
                post("/api/orders/DH01/payments/cash")
                    .with(user("customer").roles("KHACH_HANG"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amountReceived\":250000}")
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(dichVuThanhToan);
    }

    @Test
    void thuTienCanMaCsrf() throws Exception {
        kiemThuHttp
            .perform(
                post("/api/orders/DH01/payments/cash")
                    .with(user("staff").roles("NHAN_VIEN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amountReceived\":250000}")
            )
            .andExpect(status().isForbidden());
        verifyNoInteractions(dichVuThanhToan);
    }

    @Test
    void nhanVienDuocThuTien() throws Exception {
        var thanhToan = new com.cinesnack.entity.Payment(
            1L,
            1L,
            "TM01",
            "TIEN_MAT",
            new java.math.BigDecimal("229000"),
            "THANH_CONG",
            java.time.LocalDateTime.of(2026, 10, 14, 10, 0)
        );
        when(
            dichVuThanhToan.thanhToanTaiQuay("DH01", new java.math.BigDecimal("250000"), "staff")
        ).thenReturn(new PaymentService.Receipt(thanhToan, new java.math.BigDecimal("21000")));
        kiemThuHttp
            .perform(
                post("/api/orders/DH01/payments/cash")
                    .with(user("staff").roles("NHAN_VIEN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amountReceived\":250000}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment.amount").value(229000))
            .andExpect(jsonPath("$.change").value(21000));
        verify(dichVuThanhToan).thanhToanTaiQuay(
            "DH01",
            new java.math.BigDecimal("250000"),
            "staff"
        );
    }

    @Test
    void doiTenTiengVietVanGiuHopDongJson() throws Exception {
        var donHang = new com.cinesnack.entity.Order(
            1L,
            1L,
            "DH01",
            "customer@example.com",
            java.math.BigDecimal.TEN,
            "CHO_THANH_TOAN",
            null,
            java.time.LocalDateTime.of(2026, 10, 14, 10, 0)
        );
        when(dichVuDonHang.layChiTiet("DH01", "customer")).thenReturn(
            new OrderService.Details(donHang, java.util.List.of(), java.util.List.of())
        );
        kiemThuHttp
            .perform(get("/api/orders/DH01").with(user("customer").roles("KHACH_HANG")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.code").value("DH01"))
            .andExpect(jsonPath("$.order.total").value(10))
            .andExpect(jsonPath("$.tickets").isArray())
            .andExpect(jsonPath("$.snacks").isArray())
            .andExpect(jsonPath("$.donHang").doesNotExist());
    }

    @Test
    void tuChoiTienLeVaJsonSaiTruocXuLy() throws Exception {
        for (String noiDung : new String[] { "{\"amountReceived\":0.5}", "{}", "not json" }) {
            kiemThuHttp
                .perform(
                    post("/api/orders/DH01/payments/cash")
                        .with(user("staff").roles("NHAN_VIEN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noiDung)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        }
        verifyNoInteractions(dichVuThanhToan);
    }

    @Test
    void tuChoiSoLuongVaThuDienTuSai() throws Exception {
        for (String noiDung : new String[] {
            "{\"email\":\"invalid\",\"seatIds\":[],\"snacks\":[],\"combos\":[{\"comboId\":1,\"quantity\":1}]}",
            "{\"email\":\"user@example.com\",\"seatIds\":[],\"snacks\":[],\"combos\":[{\"comboId\":1,\"quantity\":11}]}",
        }) {
            kiemThuHttp
                .perform(
                    post("/api/orders")
                        .with(user("customer").roles("KHACH_HANG"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noiDung)
                )
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(dichVuDonHang);
    }

    @Test
    void tuChoiKichThuocTrangVaMaDonSai() throws Exception {
        kiemThuHttp
            .perform(get("/api/orders?size=101").with(user("customer").roles("KHACH_HANG")))
            .andExpect(status().isBadRequest());
        kiemThuHttp
            .perform(get("/api/orders/bad-code").with(user("customer").roles("KHACH_HANG")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(dichVuDonHang);
    }

    @Test
    void xacThucKiemTraMatKhauBcrypt() throws Exception {
        String matKhau = "test-only-password";
        when(khoNguoiDung.timTheoThuDienTu("staff@example.com")).thenReturn(
            new User(2, "staff@example.com", boMaHoa.encode(matKhau), "NHAN_VIEN", "HOAT_DONG")
        );
        kiemThuHttp
            .perform(get("/api/csrf").with(httpBasic("staff@example.com", matKhau)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
        kiemThuHttp
            .perform(get("/api/csrf").with(httpBasic("staff@example.com", "wrong")))
            .andExpect(status().isUnauthorized());
    }
}
