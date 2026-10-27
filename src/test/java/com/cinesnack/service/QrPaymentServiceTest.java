package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cinesnack.entity.Order;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class QrPaymentServiceTest {
    private final Clock dongHo = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final OrderService donHang = mock(OrderService.class);

    @Test
    void qrLayDungTongTienVaMaDonTuDatabase() {
        when(donHang.layDonDuocPhepTruyCap("DH123", "user@example.com"))
            .thenReturn(don("CHO_THANH_TOAN", LocalDateTime.now(dongHo).plusMinutes(10)));
        var qr = new QrPaymentService(donHang, "970436", "123456789", "Cine Snack", dongHo)
            .layQr("DH123", "user@example.com");
        assertEquals(new BigDecimal("149000.00"), qr.soTien());
        assertEquals("DH123", qr.noiDung());
        assertTrue(qr.diaChiAnh().contains("amount=149000&addInfo=DH123"));
    }

    @Test
    void khongTaoQrKhiChuaCauHinhHoacDonHetHan() {
        when(donHang.layDonDuocPhepTruyCap("DH123", "user@example.com"))
            .thenReturn(don("CHO_THANH_TOAN", LocalDateTime.now(dongHo).plusMinutes(10)));
        assertEquals(503, assertThrows(ResponseStatusException.class, () ->
            new QrPaymentService(donHang, "", "", "", dongHo).layQr("DH123", "user@example.com")
        ).getStatusCode().value());
        when(donHang.layDonDuocPhepTruyCap("DH123", "user@example.com"))
            .thenReturn(don("CHO_THANH_TOAN", LocalDateTime.now(dongHo)));
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
            new QrPaymentService(donHang, "970436", "123456789", "", dongHo).layQr("DH123", "user@example.com")
        ).getStatusCode().value());
    }

    private Order don(String status, LocalDateTime hetHan) {
        return new Order(1L, 1L, "DH123", "user@example.com", new BigDecimal("149000.00"),
            status, hetHan, LocalDateTime.now(dongHo));
    }
}
