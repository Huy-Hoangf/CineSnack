package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.catalog.repository.AdminCatalogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ShowtimeTimelineRepositoryTest {
    @Test
    void intersectsDayIncludesOvernightFiltersAndDoesNotTruncateAt100() {
        var db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:timeline;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", ""));
        db.execute("CREATE TABLE PHIM(phim_id BIGINT,ten_phim VARCHAR(200))");
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT,ten_phong VARCHAR(50))");
        db.execute("CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT,phim_id BIGINT,phong_id BIGINT,bat_dau TIMESTAMP,ket_thuc TIMESTAMP,gia_co_ban DECIMAL(12,2),phu_thu_vip DECIMAL(12,2),trang_thai VARCHAR(20),che_do_gia VARCHAR(10))");
        db.update("INSERT INTO PHIM VALUES(1,'Phim')");
        db.update("INSERT INTO PHONG_CHIEU VALUES(1,'Room 1'),(2,'Room 2')");
        var day = LocalDateTime.parse("2026-11-09T00:00:00");
        for (int i = 1; i <= 105; i++) db.update("INSERT INTO SUAT_CHIEU VALUES(?,1,1,?,?,80000,0,'SAP_CHIEU','MANUAL')", i, day.plusMinutes(i), day.plusMinutes(i + 90));
        db.update("INSERT INTO SUAT_CHIEU VALUES(106,1,2,?,?,80000,0,'HUY','MANUAL')", day.minusHours(1), day.plusHours(1));
        db.update("INSERT INTO SUAT_CHIEU VALUES(107,1,2,?,?,80000,0,'SAP_CHIEU','MANUAL')", day.plusHours(23), day.plusHours(25));
        db.update("INSERT INTO SUAT_CHIEU VALUES(108,1,2,?,?,80000,0,'SAP_CHIEU','MANUAL')", day.minusHours(2), day);
        db.update("INSERT INTO SUAT_CHIEU VALUES(109,1,2,?,?,80000,0,'SAP_CHIEU','MANUAL')", day.plusDays(1), day.plusDays(1).plusHours(2));
        var repo = new AdminCatalogRepository(db);
        var all = repo.timeline(day, day.plusDays(1), null, "");
        assertEquals(107, all.size());
        assertEquals(106, all.get(105).maSo());
        assertEquals("+07:00", all.get(105).batDau().getOffset().toString());
        assertEquals(2, repo.timeline(day, day.plusDays(1), 2L, "").size());
        assertEquals(106, repo.timeline(day, day.plusDays(1), 2L, "HUY").get(0).maSo());
        assertTrue(repo.timeline(day.plusDays(3), day.plusDays(4), null, "").isEmpty());
    }
}
