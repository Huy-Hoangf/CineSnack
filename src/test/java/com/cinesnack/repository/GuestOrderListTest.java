package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GuestOrderListTest {

    @Test
    void listsOnlySessionOrdersInDescendingOrderAndAppliesPaginationInDatabase() {
        var jdbc = new JdbcTemplate(
            new DriverManagerDataSource("jdbc:h2:mem:guest_order_list;DB_CLOSE_DELAY=-1", "sa", "")
        );
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute(
            "CREATE TABLE DON_HANG(don_hang_id BIGINT PRIMARY KEY, nguoi_dung_id BIGINT, ma_don_hang VARCHAR(30), email_nhan_ve VARCHAR(150), tong_tien DECIMAL(12,2), trang_thai VARCHAR(20), het_han_luc TIMESTAMP, tao_luc TIMESTAMP)"
        );
        jdbc.update(
            "INSERT INTO DON_HANG VALUES(1,NULL,'GUEST1','guest@example.com',10,'HUY',NULL,NULL)"
        );
        jdbc.update(
            "INSERT INTO DON_HANG VALUES(2,NULL,'GUEST2','guest@example.com',20,'HUY',NULL,NULL)"
        );
        jdbc.update(
            "INSERT INTO DON_HANG VALUES(3,7,'USER1','user@example.com',30,'HUY',NULL,NULL)"
        );
        jdbc.update(
            "INSERT INTO DON_HANG VALUES(4,NULL,'OTHER','other@example.com',40,'HUY',NULL,NULL)"
        );

        var page = new OrderRepository(jdbc).layDanhSachKhach(Set.of("GUEST1", "GUEST2", "USER1"), 0, 1);

        assertEquals(1, page.size());
        assertEquals("GUEST2", page.get(0).ma());
        assertEquals(
            "GUEST1",
            new OrderRepository(jdbc).layDanhSachKhach(Set.of("GUEST1", "GUEST2", "USER1"), 1, 1)
                .get(0)
                .ma()
        );
    }

    @Test
    void emptyGuestSessionReturnsNoOrders() {
        var jdbc = new JdbcTemplate(
            new DriverManagerDataSource("jdbc:h2:mem:guest_order_empty;DB_CLOSE_DELAY=-1", "sa", "")
        );
        assertTrue(new OrderRepository(jdbc).layDanhSachKhach(Set.of(), 0, 20).isEmpty());
    }
}
