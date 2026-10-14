package com.cinesnack.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.entity.Order;
import com.cinesnack.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OrderExpiryServiceTest {

    private final OrderRepository khoDonHang = mock(OrderRepository.class);
    private final TransactionTemplate giaoDich = mock(TransactionTemplate.class);
    private final Clock dongHo = Clock.fixed(
        Instant.parse("2026-10-13T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final LocalDateTime hienTai = LocalDateTime.now(dongHo);
    private final OrderExpiryService dichVu = new OrderExpiryService(khoDonHang, giaoDich, dongHo);

    @BeforeEach
    void chuanBi() {
        doAnswer(lanGoi -> {
            Consumer<TransactionStatus> thaoTac = lanGoi.getArgument(0);
            thaoTac.accept(mock(TransactionStatus.class));
            return null;
        })
            .when(giaoDich)
            .executeWithoutResult(any());
        when(khoDonHang.layMaDonHetHan(hienTai)).thenReturn(List.of("DH01"));
    }

    @Test
    void dongVeVaBapNuocDungHan() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("CHO_THANH_TOAN", hienTai));
        when(khoDonHang.doiTrangThai(1, "CHO_THANH_TOAN", "HET_HAN")).thenReturn(1);
        dichVu.xuLyHetHan();
        verify(khoDonHang).dongChiTiet(1, "HET_HAN");
    }

    @Test
    void khongHetHanDonVuaThanhToan() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("DA_THANH_TOAN", hienTai));
        dichVu.xuLyHetHan();
        verify(khoDonHang, never()).doiTrangThai(anyLong(), anyString(), anyString());
        verify(khoDonHang, never()).dongChiTiet(anyLong(), anyString());
    }

    @Test
    void kiemTraHanSauKhiKhoa() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(
            donHang("CHO_THANH_TOAN", hienTai.plusMinutes(1))
        );
        dichVu.xuLyHetHan();
        verify(khoDonHang, never()).dongChiTiet(anyLong(), anyString());
    }

    private Order donHang(String trangThai, LocalDateTime hetHan) {
        return new Order(
            1,
            1L,
            "DH01",
            "customer@example.com",
            BigDecimal.TEN,
            trangThai,
            hetHan,
            hienTai.minusMinutes(15)
        );
    }
}
