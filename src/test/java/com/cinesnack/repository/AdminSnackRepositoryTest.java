package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.catalog.repository.AdminSnackRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AdminSnackRepositoryTest {

    @Test
    void updatesProductAndComboIndependently() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        db.execute(
            "CREATE TABLE SAN_PHAM(san_pham_id BIGINT,ten_san_pham VARCHAR(100),loai_san_pham VARCHAR(20),gia_ban DECIMAL(12,2),trang_thai VARCHAR(20))"
        );
        db.execute(
            "CREATE TABLE COMBO(combo_id BIGINT,ten_combo VARCHAR(100),gia_ban DECIMAL(12,2),trang_thai VARCHAR(20))"
        );
        db.update(
            "INSERT INTO SAN_PHAM VALUES(1,'Bap','BAP',30000,'HOAT_DONG')"
        );
        db.update("INSERT INTO COMBO VALUES(1,'Combo',50000,'HOAT_DONG')");
        var repo = new AdminSnackRepository(db);
        assertEquals(2, repo.list().size());
        assertEquals(
            "NGUNG_HOAT_DONG",
            repo
                .update(
                    true,
                    1,
                    null,
                    null,
                    new BigDecimal("35000"),
                    "NGUNG_HOAT_DONG"
                )
                .status()
        );
        assertEquals(
            0,
            db
                .queryForObject("SELECT gia_ban FROM COMBO", BigDecimal.class)
                .compareTo(new BigDecimal("50000"))
        );
        assertNull(
            repo.update(false, 99, null, null, BigDecimal.ZERO, "HOAT_DONG")
        );
    }
}
