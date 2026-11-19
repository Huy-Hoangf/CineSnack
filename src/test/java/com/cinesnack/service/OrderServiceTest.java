package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.booking.dto.CreateOrderRequest;
import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.repository.OrderRepository;
import com.cinesnack.booking.service.OrderService;
import com.cinesnack.catalog.repository.BookingCatalogRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {

    @Test
    void staffAndAdminCannotCreateOnlineOrdersEvenThroughService() {
        for (String role : List.of("NHAN_VIEN", "QUAN_TRI")) {
            when(dichVuNguoiDung.layTaiKhoanHoatDong("worker")).thenReturn(new User(7,"worker","",role,"HOAT_DONG"));
            assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> dichVu.taoDonHang(taoYeuCau(List.of(1L)), "worker")).getStatusCode().value());
        }
        verify(danhMucDatVe, never()).timVaKhoaSuatChieu(anyLong());
    }

    @Test
    void snackBarcodeOnlyDeliversWaitingItemsAndRejectsReuse() {
        when(dichVuNguoiDung.layTaiKhoanHoatDong("staff")).thenReturn(new User(7,"staff","","NHAN_VIEN","HOAT_DONG"));
        when(khoDonHang.giaoTheoHoaDon(eq("HDB1"), eq(7L), any())).thenReturn(2, 0);
        assertEquals(2, dichVu.giaoTheoHoaDon("HDB1", "staff"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> dichVu.giaoTheoHoaDon("HDB1", "staff")).getStatusCode().value());
    }

    @Test
    void customersCannotRedeemSnackBarcode() {
        when(dichVuNguoiDung.layTaiKhoanHoatDong("customer")).thenReturn(new User(8,"customer","","KHACH_HANG","HOAT_DONG"));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> dichVu.giaoTheoHoaDon("HDB1", "customer")).getStatusCode().value());
        verify(khoDonHang, never()).giaoTheoHoaDon(anyString(), anyLong(), any());
    }

    private final OrderRepository khoDonHang = mock(OrderRepository.class);
    private final BookingCatalogRepository danhMucDatVe = mock(
        BookingCatalogRepository.class
    );
    private final UserService dichVuNguoiDung = mock(UserService.class);
    private final Clock dongHo = Clock.fixed(
        Instant.parse("2026-10-13T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final LocalDateTime hienTai = LocalDateTime.now(dongHo);
    private OrderService dichVu;

    @BeforeEach
    void chuanBi() {
        dichVu = new OrderService(
            khoDonHang,
            danhMucDatVe,
            dichVuNguoiDung,
            dongHo
        );
        when(
            dichVuNguoiDung.layTaiKhoanHoatDong("customer@example.com")
        ).thenReturn(
            new User(
                1L,
                "customer@example.com",
                "hash",
                "KHACH_HANG",
                "HOAT_DONG"
            )
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
            new BookingCatalogRepository.Seat(
                lanGoi.getArgument(0),
                2,
                "HOAT_DONG"
            )
        );
        when(danhMucDatVe.timCombo(1L)).thenReturn(
            new BookingCatalogRepository.Food(
                1,
                "Combo",
                new BigDecimal("59000"),
                "HOAT_DONG"
            )
        );
        when(
            khoDonHang.taoDonHang(
                anyLong(),
                anyString(),
                anyString(),
                any(),
                any(),
                any()
            )
        ).thenReturn(5L);
    }

    @Test
    void tinhGiaTuDanhMucVaGiuChoMuoiLamPhut() {
        var yeuCau = taoYeuCau(List.of(1L, 2L));
        Order donHang = dichVu.taoDonHang(yeuCau, "customer@example.com");
        assertEquals(0, donHang.tongTien().compareTo(new BigDecimal("219000")));
        assertEquals(hienTai.plusMinutes(15), donHang.hetHanLuc());
        verify(khoDonHang).themVe(5L, 1L, 1L, new BigDecimal("80000"));
        verify(khoDonHang).themBapNuoc(
            5L,
            null,
            1L,
            "Combo",
            1,
            new BigDecimal("59000")
        );
    }

    @Test
    void giaVipDuocTinhTheoPhuThuCuaSuatVaLuuTrenTungVe() {
        when(danhMucDatVe.timVaKhoaSuatChieu(1L)).thenReturn(
            new BookingCatalogRepository.Show(
                1,
                2,
                new BigDecimal("80000"),
                new BigDecimal("20000"),
                hienTai.plusHours(1),
                "SAP_CHIEU"
            )
        );
        when(danhMucDatVe.timGhe(2L)).thenReturn(
            new BookingCatalogRepository.Seat(2, 2, "HOAT_DONG", "VIP")
        );
        Order don = dichVu.taoDonHang(
            taoYeuCau(List.of(1L, 2L)),
            "customer@example.com"
        );
        assertEquals(0, don.tongTien().compareTo(new BigDecimal("239000")));
        verify(khoDonHang).themVe(5L, 1L, 1L, new BigDecimal("80000"));
        verify(khoDonHang).themVe(5L, 1L, 2L, new BigDecimal("100000"));
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
    void nhanVienCoTheTaoDonTaiQuay() {
        when(
            dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")
        ).thenReturn(
            new User(2L, "staff@example.com", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        Order don = dichVu.taoDonTaiQuay(
            taoYeuCau(List.of(1L)),
            "staff@example.com"
        );
        assertEquals("CHO_THANH_TOAN", don.trangThai());
        assertNull(don.maNguoiDung());
    }

    @Test
    void donTaiQuayKhongCanEmail() {
        when(
            dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")
        ).thenReturn(
            new User(2L, "staff@example.com", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        var yeuCau = new CreateOrderRequest(
            null,
            1L,
            List.of(1L),
            List.of(),
            List.of(new CreateOrderRequest.ComboChoice(1L, 1))
        );
        assertNull(
            dichVu.taoDonTaiQuay(yeuCau, "staff@example.com").thuDienTu()
        );
        verify(khoDonHang).taoDonHang(
            isNull(),
            anyString(),
            isNull(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void donOnlineVanBatBuocEmail() {
        var yeuCau = new CreateOrderRequest(
            null,
            1L,
            List.of(1L),
            List.of(),
            List.of(new CreateOrderRequest.ComboChoice(1L, 1))
        );
        assertEquals(
            400,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.taoDonHang(yeuCau, "customer@example.com")
            )
                .getStatusCode()
                .value()
        );
        assertEquals(
            400,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.taoDonKhach(yeuCau)
            )
                .getStatusCode()
                .value()
        );
    }

    @Test
    void khachKhongTheTaoDonTaiQuay() {
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.taoDonTaiQuay(
                    taoYeuCau(List.of(1L)),
                    "customer@example.com"
                )
            )
                .getStatusCode()
                .value()
        );
        verify(khoDonHang, never()).taoDonHang(
            any(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void quanTriKhongTheTaoDonTaiQuay() {
        when(
            dichVuNguoiDung.layTaiKhoanHoatDong("admin@example.com")
        ).thenReturn(
            new User(3L, "admin@example.com", "hash", "QUAN_TRI", "HOAT_DONG")
        );
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.taoDonTaiQuay(
                    taoYeuCau(List.of(1L)),
                    "admin@example.com"
                )
            )
                .getStatusCode()
                .value()
        );
        verify(khoDonHang, never()).taoDonHang(
            any(),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void chiNhanVienMoiGiaoDuocMonChuaGiao() {
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.giaoBapNuoc("DH1", 1, "customer@example.com")
            )
                .getStatusCode()
                .value()
        );
        when(
            dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")
        ).thenReturn(
            new User(2, "staff@example.com", "", "NHAN_VIEN", "HOAT_DONG")
        );
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                dichVu.giaoBapNuoc("DH1", 1, "staff@example.com")
            )
                .getStatusCode()
                .value()
        );
        verify(khoDonHang).giaoBapNuoc(eq("DH1"), eq(1L), eq(2L), any());
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
            dichVu
                .taoDonHang(taoYeuCau(List.of(1L)), "customer@example.com")
                .hetHanLuc()
        );
    }

    @Test
    void tuChoiGheTrungVaDonRong() {
        kiemTraMaTrangThai(400, taoYeuCau(List.of(1L, 1L)));
        kiemTraMaTrangThai(
            400,
            new CreateOrderRequest(
                "customer@example.com",
                null,
                List.of(),
                List.of(),
                List.of()
            )
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
            new BookingCatalogRepository.Show(
                1,
                2,
                BigDecimal.TEN,
                hienTai,
                "SAP_CHIEU"
            )
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

    @Test
    void zeroTotalOnlineSnackOrdersAreRejectedForGuestsAndCustomers() {
        when(danhMucDatVe.timCombo(1L)).thenReturn(
            new BookingCatalogRepository.Food(1, "Combo", BigDecimal.ZERO, "HOAT_DONG")
        );
        var request = new CreateOrderRequest("customer@example.com", null, List.of(),
            List.of(), List.of(new CreateOrderRequest.ComboChoice(1L, 1)));
        var guestError = assertThrows(ResponseStatusException.class, () ->
            dichVu.taoDonKhach(request));
        var customerError = assertThrows(ResponseStatusException.class, () ->
            dichVu.taoDonHang(request, "customer@example.com"));
        assertEquals(400, guestError.getStatusCode().value());
        assertEquals(400, customerError.getStatusCode().value());
        verifyNoInteractions(khoDonHang);
    }

    @Test
    void zeroTotalOnlineTicketsAreRejectedBeforeWritingOrderOrHoldingSeats() {
        when(danhMucDatVe.timVaKhoaSuatChieu(1L)).thenReturn(
            new BookingCatalogRepository.Show(1, 2, BigDecimal.ZERO,
                hienTai.plusHours(1), "SAP_CHIEU")
        );
        var request = new CreateOrderRequest("customer@example.com", 1L, List.of(1L),
            List.of(), List.of());
        assertEquals(400, assertThrows(ResponseStatusException.class, () ->
            dichVu.taoDonKhach(request)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () ->
            dichVu.taoDonHang(request, "customer@example.com")).getStatusCode().value());
        verify(khoDonHang, atLeastOnce()).gheDaDuocDat(1L, 1L);
        verifyNoMoreInteractions(khoDonHang);
    }

    @Test
    void zeroTotalCounterOrderRemainsAllowed() {
        when(dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")).thenReturn(
            new User(2L, "staff@example.com", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        when(danhMucDatVe.timCombo(1L)).thenReturn(
            new BookingCatalogRepository.Food(1, "Combo", BigDecimal.ZERO, "HOAT_DONG")
        );
        var request = new CreateOrderRequest(null, null, List.of(), List.of(),
            List.of(new CreateOrderRequest.ComboChoice(1L, 1)));
        assertEquals(0, dichVu.taoDonTaiQuay(request, "staff@example.com").tongTien().signum());
        verify(khoDonHang).taoDonHang(isNull(), anyString(), isNull(),
            eq(BigDecimal.ZERO), any(), any());
    }

    @Test
    void onlineOrderWithPaidTicketAndFreeComboRemainsAllowed() {
        when(danhMucDatVe.timCombo(1L)).thenReturn(
            new BookingCatalogRepository.Food(1, "Combo", BigDecimal.ZERO, "HOAT_DONG")
        );
        var order = dichVu.taoDonHang(taoYeuCau(List.of(1L)), "customer@example.com");
        assertEquals(0, order.tongTien().compareTo(new BigDecimal("80000")));
        verify(khoDonHang).themBapNuoc(5L, null, 1L, "Combo", 1, BigDecimal.ZERO);
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
