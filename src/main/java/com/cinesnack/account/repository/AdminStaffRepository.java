package com.cinesnack.account.repository;

import com.cinesnack.account.dto.StaffAccount;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminStaffRepository {

    private final JdbcTemplate db;

    public AdminStaffRepository(JdbcTemplate db) {
        this.db = db;
    }

    private StaffAccount row(java.sql.ResultSet r)
        throws java.sql.SQLException {
        return new StaffAccount(
            r.getLong("nguoi_dung_id"),
            r.getString("ho_ten"),
            r.getString("email"),
            r.getString("vai_tro"),
            r.getString("trang_thai")
        );
    }

    public List<StaffAccount> list() {
        return db.query(
            "SELECT nguoi_dung_id,ho_ten,email,vai_tro,trang_thai FROM NGUOI_DUNG WHERE vai_tro IN ('NHAN_VIEN','QUAN_TRI') ORDER BY nguoi_dung_id DESC",
            (r, i) -> row(r)
        );
    }

    public StaffAccount byEmail(String email) {
        return db.queryForObject(
            "SELECT nguoi_dung_id,ho_ten,email,vai_tro,trang_thai FROM NGUOI_DUNG WHERE email=?",
            (r, i) -> row(r),
            email
        );
    }

    public StaffAccount byId(long id) {
        return db.queryForObject(
            "SELECT nguoi_dung_id,ho_ten,email,vai_tro,trang_thai FROM NGUOI_DUNG WHERE nguoi_dung_id=?",
            (r, i) -> row(r),
            id
        );
    }

    public void create(String name, String email, String hash, String role) {
        db.update(
            "INSERT INTO NGUOI_DUNG(ho_ten,email,mat_khau_hash,vai_tro,trang_thai) VALUES(?,?,?,?,'HOAT_DONG')",
            name,
            email,
            hash,
            role
        );
    }

    public int status(long id, String status) {
        return db.update(
            "UPDATE NGUOI_DUNG SET trang_thai=? WHERE nguoi_dung_id=? AND vai_tro='NHAN_VIEN'",
            status,
            id
        );
    }
}
