package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.booking.repository.TicketCheckInRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TicketCheckInRepositoryTest {

    JdbcTemplate db;
    TicketCheckInRepository repo;
    final LocalDateTime start = LocalDateTime.of(2026, 11, 3, 19, 0);

    @BeforeEach
    void setup() {
        db = new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        db.execute(
            "CREATE TABLE DON_HANG(don_hang_id BIGINT PRIMARY KEY,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE HOA_DON_VE(hoa_don_ve_id BIGINT PRIMARY KEY,don_hang_id BIGINT,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT PRIMARY KEY,bat_dau TIMESTAMP,ket_thuc TIMESTAMP,trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE VE(ve_id BIGINT PRIMARY KEY,ma_ve VARCHAR(30),hoa_don_ve_id BIGINT,don_hang_id BIGINT,suat_chieu_id BIGINT,trang_thai VARCHAR(20),soat_ve_luc TIMESTAMP,nhan_vien_soat_id BIGINT,in_ve_luc TIMESTAMP,nhan_vien_in_id BIGINT)"
        );
        db.update("INSERT INTO DON_HANG VALUES(1,'DA_THANH_TOAN')");
        db.update("INSERT INTO HOA_DON_VE VALUES(1,1,'DA_THANH_TOAN')");
        db.update(
            "INSERT INTO SUAT_CHIEU VALUES(1,?,?,'SAP_CHIEU')",
            start,
            start.plusHours(2)
        );
        db.update(
            "INSERT INTO VE VALUES(1,'VEabc123',1,1,1,'DA_THANH_TOAN',NULL,NULL,NULL,NULL)"
        );
        repo = new TicketCheckInRepository(db);
    }

    @Test
    void lateCutoffIsInclusiveAndEndIsExclusive() {
        assertEquals(0, repo.checkIn("VEabc123", 7, start.plusMinutes(15).plusSeconds(1), 15));
        assertEquals(1, repo.checkIn("VEabc123", 7, start.plusMinutes(15), 15));
        assertEquals(0, repo.checkIn("VEabc123", 8, start.plusMinutes(15), 15));
    }
    @Test
    void zeroAllowsStartButNeverAfter() {
        assertEquals(0, repo.checkIn("VEabc123", 7, start.plusSeconds(1), 0));
        assertEquals(1, repo.checkIn("VEabc123", 7, start, 0));
    }
    @Test
    void configuredLimitStillCannotAdmitAtEnd() {
        assertEquals(0, repo.checkIn("VEabc123", 7, start.plusHours(2), 600));
        assertEquals(1, repo.checkIn("VEabc123", 7, start.plusHours(2).minusSeconds(1), null));
    }
    @Test
    void acceptsAtOpeningBoundaryOnlyOnceAndRecordsStaff() {
        assertEquals(1, repo.checkIn("VEABC123", 7, start.minusMinutes(30), null));
        assertEquals(0, repo.checkIn("VEabc123", 8, start, null));
        assertEquals(
            "DA_SOAT",
            db.queryForObject("SELECT trang_thai FROM VE", String.class)
        );
        assertEquals(
            7L,
            db.queryForObject("SELECT nhan_vien_soat_id FROM VE", Long.class)
        );
        assertEquals(
            start.minusMinutes(30),
            db
                .queryForObject(
                    "SELECT soat_ve_luc FROM VE",
                    java.sql.Timestamp.class
                )
                .toLocalDateTime()
        );
    }

    @Test
    void rejectsTooEarlyAndAtEndOfShow() {
        assertEquals(0, repo.checkIn("VEabc123", 7, start.minusMinutes(31), null));
        assertEquals(0, repo.checkIn("VEabc123", 7, start.plusHours(2), null));
    }

    @Test
    void rejectsUnpaidOrderOrCancelledShow() {
        db.update("UPDATE DON_HANG SET trang_thai='CHO_THANH_TOAN'");
        assertEquals(0, repo.checkIn("VEabc123", 7, start, null));
        db.update("UPDATE DON_HANG SET trang_thai='DA_THANH_TOAN'");
        db.update("UPDATE SUAT_CHIEU SET trang_thai='HUY'");
        assertEquals(0, repo.checkIn("VEabc123", 7, start, null));
    }

    @Test
    void twoConcurrentScannersCannotBothAdmitTheSameTicket() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = pool.submit(() -> {
                gate.await();
                return repo.checkIn("VEabc123", 7, start, null);
            });
            var b = pool.submit(() -> {
                gate.await();
                return repo.checkIn("VEabc123", 8, start, null);
            });
            gate.countDown();
            assertEquals(
                1,
                a.get(5, java.util.concurrent.TimeUnit.SECONDS) +
                    b.get(5, java.util.concurrent.TimeUnit.SECONDS)
            );
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void staffCanClaimOnePrintAtAnyTimeButNeverAgain() {
        assertEquals(1, repo.claimPrint("VEabc123", 7, start.minusDays(5)));
        assertEquals(0, repo.claimPrint("VEabc123", 8, start.minusDays(4)));
        assertEquals(
            start.minusDays(5),
            db
                .queryForObject(
                    "SELECT in_ve_luc FROM VE",
                    java.sql.Timestamp.class
                )
                .toLocalDateTime()
        );
        assertEquals(
            7L,
            db.queryForObject("SELECT nhan_vien_in_id FROM VE", Long.class)
        );
    }

    @Test
    void unpaidTicketCannotBePrinted() {
        db.update("UPDATE HOA_DON_VE SET trang_thai='CHO_THANH_TOAN'");
        assertEquals(0, repo.claimPrint("VEabc123", 7, start));
    }

    @Test
    void scannedTicketCannotBePrinted() {
        assertEquals(1, repo.checkIn("VEabc123", 7, start, null));
        assertEquals(0, repo.claimPrint("VEabc123", 7, start.plusHours(3)));
    }
}
