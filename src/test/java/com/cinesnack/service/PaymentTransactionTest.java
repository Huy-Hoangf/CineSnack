package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.entity.*;
import com.cinesnack.repository.*;
import java.math.BigDecimal;
import java.time.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Kiểm tra transaction Spring với H2; không thay thế kiểm thử SQL Server. */
@SpringJUnitConfig(PaymentTransactionTest.Config.class)
class PaymentTransactionTest {

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        DataSource nguonDuLieu() {
            return new DriverManagerDataSource(
                "jdbc:h2:mem:payment_tx;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            );
        }

        @Bean
        JdbcTemplate boTruyVan(DataSource nguonDuLieu) {
            return new JdbcTemplate(nguonDuLieu);
        }

        @Bean
        DataSourceTransactionManager boQuanLyGiaoDich(DataSource nguonDuLieu) {
            return new DataSourceTransactionManager(nguonDuLieu);
        }

        @Bean
        OrderRepository khoDonHang() {
            return mock(OrderRepository.class);
        }

        @Bean
        PaymentRepository khoThanhToan() {
            return mock(PaymentRepository.class);
        }

        @Bean
        InvoiceRepository khoHoaDon() {
            return mock(InvoiceRepository.class);
        }

        @Bean
        InvoiceService dichVuHoaDon(InvoiceRepository khoDuLieu) {
            return new InvoiceService(khoDuLieu, mock(OrderService.class));
        }

        @Bean
        UserService dichVuNguoiDung() {
            return mock(UserService.class);
        }

        @Bean
        Clock dongHo() {
            return Clock.fixed(
                Instant.parse("2026-10-13T03:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh")
            );
        }

        @Bean
        PaymentService dichVu(
            OrderRepository khoDonHang,
            PaymentRepository khoThanhToan,
            InvoiceService dichVuHoaDon,
            UserService dichVuNguoiDung,
            Clock dongHo
        ) {
            return new PaymentService(
                khoDonHang,
                khoThanhToan,
                dichVuHoaDon,
                dichVuNguoiDung,
                dongHo
            );
        }
    }

    @Autowired
    PaymentService dichVu;

    @Autowired
    JdbcTemplate truyVan;

    @Autowired
    OrderRepository khoDonHang;

    @Autowired
    PaymentRepository khoThanhToan;

    @Autowired
    InvoiceRepository khoHoaDon;

    @Autowired
    UserService dichVuNguoiDung;

    @Autowired
    Clock dongHo;

    @BeforeEach
    void chuanBi() {
        reset(khoDonHang, khoThanhToan, khoHoaDon, dichVuNguoiDung);
        truyVan.execute(
            "CREATE TABLE IF NOT EXISTS TX_ORDER(id BIGINT PRIMARY KEY, state VARCHAR(30))"
        );
        truyVan.execute("CREATE TABLE IF NOT EXISTS TX_PAYMENT(id BIGINT PRIMARY KEY)");
        truyVan.execute("CREATE TABLE IF NOT EXISTS TX_INVOICE(id BIGINT PRIMARY KEY)");
        truyVan.update("DELETE FROM TX_INVOICE");
        truyVan.update("DELETE FROM TX_PAYMENT");
        truyVan.update("DELETE FROM TX_ORDER");
        truyVan.update("INSERT INTO TX_ORDER VALUES (1, 'CHO_THANH_TOAN')");
        LocalDateTime hienTai = LocalDateTime.now(dongHo);
        when(dichVuNguoiDung.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(2, "staff", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(
            new Order(
                1,
                1L,
                "DH01",
                "email@example.com",
                BigDecimal.TEN,
                "CHO_THANH_TOAN",
                hienTai.plusMinutes(15),
                hienTai
            )
        );
        when(khoDonHang.coTheThanhToan(1, hienTai)).thenReturn(true);
        when(khoDonHang.tinhTongChiTiet(1)).thenReturn(BigDecimal.TEN);
        when(khoThanhToan.taoThanhToanTienMat(eq(1L), any(), any())).thenAnswer(lanGoi -> {
            truyVan.update("INSERT INTO TX_PAYMENT VALUES (1)");
            return new Payment(1, 1, "TM01", "TIEN_MAT", BigDecimal.TEN, "THANH_CONG", hienTai);
        });
        when(khoDonHang.doiTrangThai(1, "CHO_THANH_TOAN", "DA_THANH_TOAN")).thenAnswer(lanGoi ->
            truyVan.update("UPDATE TX_ORDER SET state = 'DA_THANH_TOAN' WHERE id = 1")
        );
        doAnswer(lanGoi -> {
            truyVan.update("INSERT INTO TX_INVOICE VALUES (1)");
            return null;
        })
            .when(khoHoaDon)
            .taoHoaDon(any(), any());
    }

    @Test
    void loiHoaDonHoanTacTatCa() {
        doAnswer(lanGoi -> {
            truyVan.update("INSERT INTO TX_INVOICE VALUES (1)");
            throw new IllegalStateException("invoice failed after inserting header");
        })
            .when(khoHoaDon)
            .taoHoaDon(any(), any());
        assertThrows(IllegalStateException.class, () ->
            dichVu.thanhToanTaiQuay("DH01", BigDecimal.TEN, "staff")
        );
        assertEquals(0, truyVan.queryForObject("SELECT COUNT(*) FROM TX_PAYMENT", Integer.class));
        assertEquals(0, truyVan.queryForObject("SELECT COUNT(*) FROM TX_INVOICE", Integer.class));
        assertEquals(
            "CHO_THANH_TOAN",
            truyVan.queryForObject("SELECT state FROM TX_ORDER WHERE id = 1", String.class)
        );
    }

    @Test
    void thanhToanThanhCongLuuTatCa() {
        dichVu.thanhToanTaiQuay("DH01", BigDecimal.TEN, "staff");
        assertEquals(1, truyVan.queryForObject("SELECT COUNT(*) FROM TX_PAYMENT", Integer.class));
        assertEquals(1, truyVan.queryForObject("SELECT COUNT(*) FROM TX_INVOICE", Integer.class));
        assertEquals(
            "DA_THANH_TOAN",
            truyVan.queryForObject("SELECT state FROM TX_ORDER WHERE id = 1", String.class)
        );
    }
}
