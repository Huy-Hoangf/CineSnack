package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.entity.Order;
import com.cinesnack.entity.Payment;
import com.cinesnack.entity.User;
import com.cinesnack.repository.OrderRepository;
import com.cinesnack.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PaymentServiceTest {

    private final OrderRepository khoDonHang = mock(OrderRepository.class);
    private final PaymentRepository khoThanhToan = mock(PaymentRepository.class);
    private final InvoiceService dichVuHoaDon = mock(InvoiceService.class);
    private final UserService dichVuNguoiDung = mock(UserService.class);
    private final Clock dongHo = Clock.fixed(
        Instant.parse("2026-10-13T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final LocalDateTime hienTai = LocalDateTime.now(dongHo);
    private PaymentService dichVu;

    @BeforeEach
    void chuanBi() {
        dichVu = new PaymentService(
            khoDonHang,
            khoThanhToan,
            dichVuHoaDon,
            dichVuNguoiDung,
            dongHo
        );
        when(dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")).thenReturn(
            new User(2L, "staff@example.com", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(
            donHang("CHO_THANH_TOAN", hienTai.plusMinutes(10))
        );
        when(khoDonHang.coTheThanhToan(1L, hienTai)).thenReturn(true);
        when(khoDonHang.tinhTongChiTiet(1L)).thenReturn(new BigDecimal("229000.00"));
        when(khoDonHang.doiTrangThai(1L, "CHO_THANH_TOAN", "DA_THANH_TOAN")).thenReturn(1);
        when(khoThanhToan.taoThanhToanTienMat(eq(1L), any(), eq(hienTai))).thenReturn(giaoDich());
    }

    @Test
    void thuDungTongTienVaTraTienThua() {
        var ketQua = dichVu.thanhToanTaiQuay("DH01", new BigDecimal("250000"), "staff@example.com");
        assertEquals(0, ketQua.tienThua().compareTo(new BigDecimal("21000")));
        verify(khoThanhToan).taoThanhToanTienMat(1L, new BigDecimal("229000.00"), hienTai);
        verify(khoDonHang).xacNhanChiTietDaThanhToan(1L);
        verify(dichVuHoaDon).phatHanh(donHang("CHO_THANH_TOAN", hienTai.plusMinutes(10)), hienTai);
    }

    @Test
    void nhanVienXacNhanChuyenKhoanDungTongTien() {
        when(khoThanhToan.taoThanhToanChuyenKhoan(1L, new BigDecimal("229000.00"), "BANK123", hienTai))
            .thenReturn(new Payment(2L, 1L, "BANK123", "CHUYEN_KHOAN", new BigDecimal("229000.00"), "THANH_CONG", hienTai));
        var giaoDich = dichVu.xacNhanChuyenKhoan("DH01", "BANK123", new BigDecimal("229000"), "staff@example.com");
        assertEquals("CHUYEN_KHOAN", giaoDich.phuongThuc());
        verify(dichVuHoaDon).phatHanh(any(), eq(hienTai));
    }

    @Test
    void tuChoiChuyenKhoanKhacTongTien() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () ->
            dichVu.xacNhanChuyenKhoan("DH01", "BANK124", new BigDecimal("230000"), "staff@example.com")
        ).getStatusCode().value());
        verify(khoThanhToan, never()).taoThanhToanChuyenKhoan(anyLong(), any(), anyString(), any());
    }

    @Test
    void khachKhongTheXacNhanChuyenKhoan() {
        when(dichVuNguoiDung.layTaiKhoanHoatDong("customer@example.com")).thenReturn(
            new User(3L, "customer@example.com", "hash", "KHACH_HANG", "HOAT_DONG")
        );
        assertEquals(403, assertThrows(ResponseStatusException.class, () ->
            dichVu.xacNhanChuyenKhoan("DH01", "BANK124", new BigDecimal("229000"), "customer@example.com")
        ).getStatusCode().value());
        verify(khoDonHang, never()).timVaKhoa(anyString());
    }

    @Test
    void tuChoiXacNhanTienMatLapDeKhongTinhLaiTienThua() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(
            donHang("DA_THANH_TOAN", hienTai.minusMinutes(1))
        );
        kiemTraMaTrangThai(409, new BigDecimal("300000"));
        verifyNoInteractions(khoThanhToan, dichVuHoaDon);
    }

    @Test
    void chuyenKhoanLapChiTraGiaoDichDungPhuongThucMaVaSoTien() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("DA_THANH_TOAN", hienTai.minusMinutes(1)));
        Payment daCo = new Payment(2L, 1L, "BANK123", "CHUYEN_KHOAN", new BigDecimal("229000.00"), "THANH_CONG", hienTai);
        when(khoThanhToan.timThanhToanThanhCong(1L)).thenReturn(daCo);
        assertEquals(daCo, dichVu.xacNhanChuyenKhoan("DH01", "BANK123", new BigDecimal("229000"), "staff@example.com"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
            dichVu.xacNhanChuyenKhoan("DH01", "BANK123", new BigDecimal("230000"), "staff@example.com")
        ).getStatusCode().value());
        when(khoThanhToan.timThanhToanThanhCong(1L)).thenReturn(giaoDich());
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
            dichVu.xacNhanChuyenKhoan("DH01", "TM01", new BigDecimal("229000"), "staff@example.com")
        ).getStatusCode().value());
        when(khoThanhToan.timThanhToanThanhCong(1L)).thenReturn(
            new Payment(3L, 1L, "BANK123", "CHUYEN_KHOAN", new BigDecimal("228000"), "THANH_CONG", hienTai)
        );
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
            dichVu.xacNhanChuyenKhoan("DH01", "BANK123", new BigDecimal("229000"), "staff@example.com")
        ).getStatusCode().value());
        verify(khoThanhToan, never()).taoThanhToanChuyenKhoan(anyLong(), any(), anyString(), any());
    }

    @Test
    void khongTraBienLaiTienMatChoDonDaChuyenKhoan() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("DA_THANH_TOAN", hienTai.minusMinutes(1)));
        when(khoThanhToan.timThanhToanThanhCong(1L)).thenReturn(
            new Payment(2L, 1L, "BANK123", "CHUYEN_KHOAN", new BigDecimal("229000"), "THANH_CONG", hienTai)
        );
        kiemTraMaTrangThai(409, new BigDecimal("250000"));
    }

    @Test
    void tuChoiDungThoiDiemHetHan() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("CHO_THANH_TOAN", hienTai));
        kiemTraMaTrangThai(409, new BigDecimal("250000"));
        verifyNoInteractions(khoThanhToan, dichVuHoaDon);
    }

    @Test
    void tuChoiDonDaHuy() {
        when(khoDonHang.timVaKhoa("DH01")).thenReturn(donHang("HUY", hienTai.plusMinutes(1)));
        kiemTraMaTrangThai(409, new BigDecimal("250000"));
    }

    @Test
    void tuChoiTienKhongDu() {
        kiemTraMaTrangThai(400, new BigDecimal("228999"));
        verifyNoInteractions(khoThanhToan, dichVuHoaDon);
    }

    @Test
    void tuChoiTienLeVaThieuTien() {
        kiemTraMaTrangThai(400, new BigDecimal("250000.50"));
        kiemTraMaTrangThai(400, null);
    }

    @Test
    void khachKhongDuocXacNhanTienMat() {
        when(dichVuNguoiDung.layTaiKhoanHoatDong("staff@example.com")).thenReturn(
            new User(2L, "staff@example.com", "hash", "KHACH_HANG", "HOAT_DONG")
        );
        kiemTraMaTrangThai(403, new BigDecimal("250000"));
        verifyNoInteractions(khoDonHang, khoThanhToan, dichVuHoaDon);
    }

    @Test
    void tuChoiTongTienSaiHoacSuatKhongHopLe() {
        when(khoDonHang.tinhTongChiTiet(1L)).thenReturn(BigDecimal.ONE);
        kiemTraMaTrangThai(409, new BigDecimal("250000"));
        when(khoDonHang.tinhTongChiTiet(1L)).thenReturn(new BigDecimal("229000.00"));
        when(khoDonHang.coTheThanhToan(1L, hienTai)).thenReturn(false);
        kiemTraMaTrangThai(409, new BigDecimal("250000"));
    }

    @Test
    void loiHoaDonDuocTruyenDeHoanTac() {
        doThrow(new IllegalStateException("invoice failed"))
            .when(dichVuHoaDon)
            .phatHanh(any(), any());
        assertThrows(IllegalStateException.class, () ->
            dichVu.thanhToanTaiQuay("DH01", new BigDecimal("250000"), "staff@example.com")
        );
    }

    private void kiemTraMaTrangThai(int trangThai, BigDecimal soTien) {
        var loi = assertThrows(ResponseStatusException.class, () ->
            dichVu.thanhToanTaiQuay("DH01", soTien, "staff@example.com")
        );
        assertEquals(trangThai, loi.getStatusCode().value());
    }

    private Order donHang(String trangThai, LocalDateTime hetHan) {
        return new Order(
            1L,
            1L,
            "DH01",
            "customer@example.com",
            new BigDecimal("229000.00"),
            trangThai,
            hetHan,
            hienTai.minusMinutes(5)
        );
    }

    private Payment giaoDich() {
        return new Payment(
            1L,
            1L,
            "TM01",
            "TIEN_MAT",
            new BigDecimal("229000.00"),
            "THANH_CONG",
            hienTai
        );
    }
}
