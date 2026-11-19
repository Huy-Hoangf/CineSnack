package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.catalog.repository.AdminReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AdminReportRepositoryTest {

    @Test
    void countsPaidOrderOnceAndUsesSoldItemSnapshots() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:" +
                    UUID.randomUUID() +
                    ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        db.execute(
            "CREATE TABLE DON_HANG(don_hang_id BIGINT,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE THANH_TOAN(don_hang_id BIGINT,trang_thai VARCHAR(20),thanh_toan_luc TIMESTAMP)"
        );
        db.execute(
            "CREATE TABLE VE(don_hang_id BIGINT,trang_thai VARCHAR(20),don_gia DECIMAL(12,2))"
        );
        db.execute(
            "CREATE TABLE CHI_TIET_HOA_DON_BONG_NUOC(don_hang_id BIGINT,trang_thai_phuc_vu VARCHAR(20),so_luong INT,don_gia DECIMAL(12,2))"
        );
        db.update(
            "INSERT INTO DON_HANG VALUES(1,'DA_THANH_TOAN'),(2,'CHO_THANH_TOAN')"
        );
        db.update(
            "INSERT INTO THANH_TOAN VALUES(1,'THANH_CONG','2026-11-04 10:00:00'),(1,'THANH_CONG','2026-11-05 10:01:00'),(2,'THAT_BAI','2026-11-04 10:00:00')"
        );
        db.update(
            "INSERT INTO VE VALUES(1,'DA_SOAT',80000),(2,'CHO_THANH_TOAN',90000)"
        );
        db.update(
            "INSERT INTO CHI_TIET_HOA_DON_BONG_NUOC VALUES(1,'DA_NHAN',2,30000)"
        );
        var report = new AdminReportRepository(db).daily(
            LocalDate.of(2026, 11, 4)
        );
        assertEquals(1, report.orders());
        assertEquals(1, report.tickets());
        assertEquals(2, report.snacks());
        assertEquals(
            0,
            report.totalRevenue().compareTo(new BigDecimal("140000"))
        );
        assertEquals(
            0,
            new AdminReportRepository(db)
                .daily(LocalDate.of(2026, 11, 5))
                .orders()
        );
    }
}
