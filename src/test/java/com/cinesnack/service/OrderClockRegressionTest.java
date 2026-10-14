package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.entity.User;
import com.cinesnack.repository.BookingCatalogRepository;
import com.cinesnack.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OrderClockRegressionTest {

    @Test
    void tuChoiSuatDaBatDauTrongLucChoKhoa() {
        var khoDon = mock(OrderRepository.class);
        var danhMuc = mock(BookingCatalogRepository.class);
        var nguoiDung = mock(UserService.class);
        var dongHo = new MutableClock(Instant.parse("2026-10-14T03:00:00Z"));
        var batDau = LocalDateTime.now(dongHo).plusSeconds(5);
        when(nguoiDung.layTaiKhoanHoatDong("customer@example.com")).thenReturn(
            new User(1, "customer@example.com", "hash", "KHACH_HANG", "HOAT_DONG")
        );
        when(danhMuc.timVaKhoaSuatChieu(1L)).thenAnswer(lanGoi -> {
            // Simulate time spent waiting for another transaction's show lock.
            dongHo.tienThem(Duration.ofSeconds(10));
            return new BookingCatalogRepository.Show(
                1,
                2,
                new BigDecimal("80000"),
                batDau,
                "SAP_CHIEU"
            );
        });
        when(danhMuc.timGhe(1L)).thenReturn(new BookingCatalogRepository.Seat(1, 2, "HOAT_DONG"));
        var dichVu = new OrderService(khoDon, danhMuc, nguoiDung, dongHo);
        var yeuCau = new CreateOrderRequest(
            "customer@example.com",
            1L,
            List.of(1L),
            List.of(),
            List.of()
        );

        var loi = assertThrows(ResponseStatusException.class, () ->
            dichVu.taoDonHang(yeuCau, "customer@example.com")
        );

        assertEquals(409, loi.getStatusCode().value());
        verify(khoDon, never()).taoDonHang(
            anyLong(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        );
    }

    private static final class MutableClock extends Clock {

        private Instant thoiDiem;
        private final ZoneId muiGio;

        MutableClock(Instant thoiDiem) {
            this(thoiDiem, ZoneId.of("Asia/Ho_Chi_Minh"));
        }

        private MutableClock(Instant thoiDiem, ZoneId muiGio) {
            this.thoiDiem = thoiDiem;
            this.muiGio = muiGio;
        }

        void tienThem(Duration khoangThoiGian) {
            thoiDiem = thoiDiem.plus(khoangThoiGian);
        }

        @Override
        public ZoneId getZone() {
            return muiGio;
        }

        @Override
        public Clock withZone(ZoneId muiGio) {
            return new MutableClock(thoiDiem, muiGio);
        }

        @Override
        public Instant instant() {
            return thoiDiem;
        }
    }
}
