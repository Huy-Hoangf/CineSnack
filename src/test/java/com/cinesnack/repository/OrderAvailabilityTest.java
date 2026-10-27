package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrderAvailabilityTest {

    @Test
    void khongThuTienKhiPhongBiKhoaHoacPhimNgungChieu() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:availability;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE TABLE PHIM(phim_id BIGINT PRIMARY KEY,trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT PRIMARY KEY,trang_thai VARCHAR(20))");
        db.execute(
            "CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT PRIMARY KEY,phim_id BIGINT,phong_id BIGINT,bat_dau TIMESTAMP,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE GHE(ghe_id BIGINT PRIMARY KEY,phong_id BIGINT,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE VE(don_hang_id BIGINT,suat_chieu_id BIGINT,ghe_id BIGINT,don_gia DECIMAL(12,2),trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE CHI_TIET_HOA_DON_BONG_NUOC(don_hang_id BIGINT,so_luong INT,don_gia DECIMAL(12,2),trang_thai_phuc_vu VARCHAR(20))"
        );
        db.update("INSERT INTO PHIM VALUES(1,'DANG_CHIEU')");
        db.update("INSERT INTO PHONG_CHIEU VALUES(1,'HOAT_DONG')");
        db.update(
            "INSERT INTO SUAT_CHIEU VALUES(1,1,1,TIMESTAMP '2026-10-14 13:00:00','SAP_CHIEU')"
        );
        db.update("INSERT INTO GHE VALUES(1,1,'HOAT_DONG')");
        db.update("INSERT INTO VE VALUES(1,1,1,80000,'CHO_THANH_TOAN')");
        var kho = new OrderRepository(db);
        var luc = LocalDateTime.of(2026, 10, 14, 12, 0);
        assertTrue(kho.coTheThanhToan(1, luc));
        db.update("UPDATE PHONG_CHIEU SET trang_thai='KHOA'");
        assertFalse(kho.coTheThanhToan(1, luc));
        db.update("UPDATE PHONG_CHIEU SET trang_thai='HOAT_DONG'");
        db.update("UPDATE PHIM SET trang_thai='NGUNG_CHIEU'");
        assertFalse(kho.coTheThanhToan(1, luc));
    }
}
