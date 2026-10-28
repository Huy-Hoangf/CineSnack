package com.cinesnack.repository;

import com.cinesnack.booking.repository.OrderRepository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrderAdminSearchTest {

    @Test
    void searchFiltersAllGuestAndUserOrdersBeforePagination() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:admin_order_search;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        );
        db.execute("DROP ALL OBJECTS");
        db.execute(
            "CREATE TABLE DON_HANG(don_hang_id BIGINT PRIMARY KEY, nguoi_dung_id BIGINT, ma_don_hang VARCHAR(30), email_nhan_ve VARCHAR(150), tong_tien DECIMAL(12,2), trang_thai VARCHAR(20), het_han_luc TIMESTAMP, tao_luc TIMESTAMP)"
        );
        db.update(
            "INSERT INTO DON_HANG VALUES(1,NULL,'DHGUEST','guest@example.com',75000,'CHO_THANH_TOAN',NULL,CURRENT_TIMESTAMP),(2,9,'DHUSER','guest@example.com',75000,'DA_THANH_TOAN',NULL,CURRENT_TIMESTAMP),(3,9,'DHNEW','other@example.com',75000,'CHO_THANH_TOAN',NULL,CURRENT_TIMESTAMP)"
        );
        var repository = new OrderRepository(db);

        assertEquals("DHGUEST", repository.layTatCa("DHGUEST", "", 0, 1).get(0).ma());
        assertNull(
            repository.layTatCa("guest@example.com", "CHO_THANH_TOAN", 0, 1).get(0).maNguoiDung()
        );
        assertEquals(
            "DHUSER",
            repository.layTatCa("guest@example.com", "DA_THANH_TOAN", 0, 1).get(0).ma()
        );
        assertEquals("DHGUEST", repository.layTatCa("guest@example.com", "", 1, 1).get(0).ma());
        assertTrue(repository.layTatCa("' OR 1=1 --", "", 0, 20).isEmpty());
    }
}
