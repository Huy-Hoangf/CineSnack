package com.cinesnack.service;

import com.cinesnack.entity.*;
import com.cinesnack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository khoDuLieu = mock(UserRepository.class);
    private final UserService dichVuNguoiDung = new UserService(khoDuLieu);
    private final Order donHang = new Order(1, 1L, "DH01", "customer@example.com", BigDecimal.TEN,
            "CHO_THANH_TOAN", LocalDateTime.now(), LocalDateTime.now());

    @Test
    void khachChiXemDonCuaMinh() {
        assertDoesNotThrow(() -> dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung(1, "KHACH_HANG"), donHang));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung(2, "KHACH_HANG"), donHang)).getStatusCode().value());
    }

    @Test
    void nhanVienDuocXemDonVaBaoDonKhongTonTai() {
        assertDoesNotThrow(() -> dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung(2, "NHAN_VIEN"), donHang));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung(1, "KHACH_HANG"), null)).getStatusCode().value());
    }

    @Test
    void tuChoiTaiKhoanBiKhoa() {
        when(khoDuLieu.timTheoThuDienTu("user@example.com"))
                .thenReturn(new User(1, "user@example.com", "hash", "KHACH_HANG", "KHOA"));
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> dichVuNguoiDung.layTaiKhoanHoatDong("user@example.com")).getStatusCode().value());
    }

    private User nguoiDung(long maSo, String vaiTro) {
        return new User(maSo, "user@example.com", "hash", vaiTro, "HOAT_DONG");
    }
}
