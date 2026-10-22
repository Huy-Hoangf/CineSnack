package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrderTicketDetailsTest {

    @Test
    void pendingTicketIncludesMovieRoomAndVietnamStartTime() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource("jdbc:h2:mem:ticket_details;DB_CLOSE_DELAY=-1", "sa", "")
        );
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE TABLE PHIM(phim_id BIGINT PRIMARY KEY, ten_phim VARCHAR(200))");
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT PRIMARY KEY, ten_phong VARCHAR(50))");
        db.execute(
            "CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT PRIMARY KEY, phim_id BIGINT, phong_id BIGINT, bat_dau TIMESTAMP)"
        );
        db.execute("CREATE TABLE GHE(ghe_id BIGINT PRIMARY KEY, ma_ghe VARCHAR(10))");
        db.execute("CREATE TABLE HOA_DON_VE(hoa_don_ve_id BIGINT PRIMARY KEY, don_hang_id BIGINT)");
        db.execute(
            "CREATE TABLE VE(ve_id BIGINT, hoa_don_ve_id BIGINT, suat_chieu_id BIGINT, ghe_id BIGINT, ma_ve VARCHAR(30), don_gia DECIMAL(12,2), trang_thai VARCHAR(20))"
        );
        db.update("INSERT INTO PHIM VALUES(1,'Phim da chon')");
        db.update("INSERT INTO PHONG_CHIEU VALUES(2,'Phong 2')");
        db.update("INSERT INTO SUAT_CHIEU VALUES(3,1,2,TIMESTAMP '2026-10-15 19:30:00')");
        db.update("INSERT INTO GHE VALUES(4,'A1')");
        db.update("INSERT INTO HOA_DON_VE VALUES(5,6)");
        db.update("INSERT INTO VE VALUES(7,5,3,4,'VE01',75000,'CHO_THANH_TOAN')");

        var ticket = new OrderRepository(db).layVe(6).get(0);

        assertEquals("Phim da chon", ticket.tenPhim());
        assertEquals("Phong 2", ticket.tenPhong());
        assertEquals(OffsetDateTime.parse("2026-10-15T19:30:00+07:00"), ticket.batDau());
        assertEquals("CHO_THANH_TOAN", ticket.trangThai());
        assertEquals("A1", ticket.nhanGhe());
    }
}
