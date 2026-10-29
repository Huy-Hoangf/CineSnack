package com.cinesnack.account.repository;

import com.cinesnack.account.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate truyVan;

    public UserRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public void taoKhachHang(String hoTen, String email, String matKhauHash) {
        truyVan.update("""
            INSERT INTO NGUOI_DUNG (ho_ten, email, mat_khau_hash, vai_tro, trang_thai)
            VALUES (?, ?, ?, 'KHACH_HANG', 'HOAT_DONG')
            """, hoTen, email, matKhauHash);
    }

    public User timTheoThuDienTu(String thuDienTu) {
        var dsNguoiDung = truyVan.query(
            """
            SELECT nguoi_dung_id, email, mat_khau_hash, vai_tro, trang_thai
            FROM NGUOI_DUNG WHERE email = ?
            """,
            (dong, viTri) ->
                new User(
                    dong.getLong("nguoi_dung_id"),
                    dong.getString("email"),
                    dong.getString("mat_khau_hash"),
                    dong.getString("vai_tro"),
                    dong.getString("trang_thai")
                ),
            thuDienTu
        );
        return dsNguoiDung.isEmpty() ? null : dsNguoiDung.get(0);
    }
}
