package com.cinesnack.service;

import com.cinesnack.entity.Order;
import com.cinesnack.entity.User;
import com.cinesnack.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository khoNguoiDung;

    public UserService(UserRepository khoNguoiDung) { this.khoNguoiDung = khoNguoiDung; }

    public User layTaiKhoanHoatDong(String thuDienTu) {
        User nguoiDung = khoNguoiDung.timTheoThuDienTu(thuDienTu);
        if (nguoiDung == null || !"HOAT_DONG".equals(nguoiDung.trangThai())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hoạt động");
        }
        return nguoiDung;
    }

    public void kiemTraQuyenTruyCapDon(User nguoiDung, Order donHang) {
        if (donHang == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        if (!nguoiDung.laNhanVien() && !Long.valueOf(nguoiDung.maSo()).equals(donHang.maNguoiDung())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này");
        }
    }
}
