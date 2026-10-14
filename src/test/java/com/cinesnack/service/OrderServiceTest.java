package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.entity.Order;
import com.cinesnack.entity.User;
import com.cinesnack.repository.BookingCatalogRepository;
import com.cinesnack.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {

    private final OrderRepository khoDonHang = mock(OrderRepository.class);
    private final BookingCatalogRepository danhMucDatVe = mock(BookingCatalogRepository.class);
    private final UserService dichVuNguoiDung = mock(UserService.class);
    private final Clock dongHo = Clock.fixed(
        Instant.parse("2026-10-13T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final LocalDateTime hienTai = LocalDateTime.now(dongHo);
    private OrderService dichVu;

    @BeforeEach
    void chuanBi() {
        dichVu = new OrderService(khoDonHang, danhMucDatVe, dichVuNguoiDung, dongHo);
        when(dichVuNguoiDung.layTaiKhoanHoatDong("customer@example.com")).thenReturn(
            new User(1L, "customer@example.com", "hash", "KHACH_HANG", "HOAT_DONG")
        );
        when(danhMucDatVe.timVaKhoaSuatChieu(1L)).thenReturn(
            new BookingCatalogRepository.Show(
                1,
                2,
                new BigDecimal("80000"),
                hienTai.plusHours(1),
                "SAP_CHIEU"
            )
        );
        when(danhMucDatVe.timGhe(anyLong())).thenAnswer(lanGoi ->
            new BookingCatalogRepository.Seat(lanGoi.getArgument(0), 2, "HOAT_DONG")
        );
        when(danhMucDatVe.timCombo(1L)).thenReturn(
            new BookingCatalogRepository.Food(1, "Combo", new BigDecimal("59000"), "HOAT_DONG")
        );
        when(
            khoDonHang.taoDonHang(anyLong(), anyString(), anyString(), any(), any(), any())
        ).thenReturn(5L);
    }

    @Test
    void tinhGiaTuDanhMucVaGiuChoMuoiLamPhut() {
        var yeuCau = taoYeuCau(List.of(1L, 2L));
        Order donHang = dichVu.taoDonHang(yeuCau, "customer@example.com");
        assertEquals(0, donHang.tongTien().compareTo(new BigDecimal("219000")));
        assertEquals(hienTai.plusMinutes(15), donHang.hetHanLuc());
        verify(khoDonHang).themVe(5L, 1L, 1L, new BigDecimal("80000"));
        verify(khoDonHang).themBapNuoc(5L, null, 1L, "Combo", 1, new BigDecimal("59000"));
    }

    @Test
    void choPhepDonChiBapNuoc() {
        var yeuCau = new CreateOrderRequest(
            "customer@example.com",
            null,
            List.of(),
            List.of(),
            List.of(new CreateOrderRequest.ComboChoice(1L, 1))
        );
        assertEquals(
            0,
            dichVu
                .taoDonHang(yeuCau, "customer@example.com")
                .tongTien()
                .compareTo(new BigDecimal("59000"))
        );
        verify(danhMucDatVe, never()).timVaKhoaSuatChieu(anyLong());
    }

    @Test
    void giuChoKhongQuaGioChieu() {
        when(danhMucDatVe.timVaKhoaSuatChieu(1L)).thenReturn(
            new BookingCatalogRepository.Show(
                1,
                2,
                new BigDecimal("80000"),
                hienTai.plusMinutes(5),
                "SAP_CHIEU"
            )
        );
        assertEquals(
            hienTai.plusMinutes(5),
            dichVu.taoDonHang(taoYeuCau(List.of(1L)), "customer@example.com").hetHanLuc()
        );
    }

    @Test
    void tuChoiGheTrungVaDonRong() {
        kiemTraMaTrangThai(400, taoYeuCau(List.of(1L, 1L)));
        kiemTraMaTrangThai(
            400,
            new CreateOrderRequest("customer@example.com", null, List.of(), List.of(), List.of())
        );
        verify(khoDonHang, never()).taoDonHang(
            anyLong(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void tuChoiGheSaiPhongHoacDaDat() {
        when(danhMucDatVe.timGhe(1L)).thenReturn(
            new BookingCatalogRepository.Seat(1, 3, "HOAT_DONG")
        );
        kiemTraMaTrangThai(400, taoYeuCau(List.of(1L)));
        when(danhMucDatVe.timGhe(1L)).thenReturn(
            new BookingCatalogRepository.Seat(1, 2, "HOAT_DONG")
        );
        when(khoDonHang.gheDaDuocDat(1, 1)).thenReturn(true);
        kiemTraMaTrangThai(409, taoYeuCau(List.of(1L)));
    }

    @Test
    void tuChoiSuatDaBatDau() {
        when(danhMucDatVe.timVaKhoaSuatChieu(1L)).thenReturn(
            new BookingCatalogRepository.Show(1, 2, BigDecimal.TEN, hienTai, "SAP_CHIEU")
        );
        kiemTraMaTrangThai(409, taoYeuCau(List.of(1L)));
    }

    @Test
    void khongHuyDonDaThanhToan() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(
            new Order(
                1,
                1L,
                "DH01",
                "customer@example.com",
                BigDecimal.TEN,
                "DA_THANH_TOAN",
                hienTai.plusMinutes(15),
                hienTai
            )
        );
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.huyDonHang("DH01", "customer@example.com")
            )
                .getStatusCode()
                .value()
        );
        verify(khoDonHang, never()).dongChiTiet(anyLong(), anyString());
    }

    @Test
    void tuChoiSanPhamHoacComboThieuMa() {
        kiemTraMaTrangThai(
            400,
            new CreateOrderRequest(
                "customer@example.com",
                null,
                List.of(),
                List.of(new CreateOrderRequest.SnackChoice(null, 1)),
                List.of()
            )
        );
        kiemTraMaTrangThai(
            400,
            new CreateOrderRequest(
                "customer@example.com",
                null,
                List.of(),
                List.of(),
                List.of(new CreateOrderRequest.ComboChoice(null, 1))
            )
        );
    }

    private CreateOrderRequest taoYeuCau(List<Long> dsGhe) {
        return new CreateOrderRequest(
            "customer@example.com",
            1L,
            dsGhe,
            List.of(),
            List.of(new CreateOrderRequest.ComboChoice(1L, 1))
        );
    }

    private void kiemTraMaTrangThai(int trangThai, CreateOrderRequest yeuCau) {
        assertEquals(
            trangThai,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.taoDonHang(yeuCau, "customer@example.com")
            )
                .getStatusCode()
                .value()
        );
    }
}
