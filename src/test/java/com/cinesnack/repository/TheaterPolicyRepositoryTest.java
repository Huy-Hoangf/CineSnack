package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.catalog.dto.TheaterPolicy;
import com.cinesnack.catalog.repository.TheaterPolicyRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TheaterPolicyRepositoryTest {
    @Test
    void roundTripsPricesAndNullablePolicyWithoutChangingSavedShowPrices() {
        var db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT PRIMARY KEY, trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE BANG_GIA_PHONG(phong_id BIGINT PRIMARY KEY REFERENCES PHONG_CHIEU(phong_id) ON DELETE CASCADE, gia_thuong_ngay_thuong DECIMAL(12,2),gia_vip_ngay_thuong DECIMAL(12,2),gia_thuong_cuoi_tuan DECIMAL(12,2),gia_vip_cuoi_tuan DECIMAL(12,2))");
        db.execute("CREATE TABLE CHINH_SACH_RAP(chinh_sach_id INT PRIMARY KEY,gioi_han_soat_muon_phut INT)");
        db.execute("CREATE TABLE SUAT_CHIEU(gia_co_ban DECIMAL(12,2),phu_thu_vip DECIMAL(12,2),che_do_gia VARCHAR(10))");
        db.update("INSERT INTO PHONG_CHIEU VALUES(1,'HOAT_DONG'),(2,'DA_XOA')");
        db.update("INSERT INTO CHINH_SACH_RAP VALUES(1,NULL)");
        db.update("INSERT INTO SUAT_CHIEU VALUES(70000,20000,'POLICY')");
        var repo = new TheaterPolicyRepository(db);
        assertTrue(repo.roomExists(1));
        assertFalse(repo.roomExists(2));
        assertNull(repo.pricing(1));
        var initial = new TheaterPolicy.RoomPricing(new BigDecimal("70000"),new BigDecimal("90000"),new BigDecimal("100000"),new BigDecimal("130000"));
        repo.savePricing(1, initial);
        assertEquals(0, initial.weekdayRegular().compareTo(repo.pricing(1).weekdayRegular()));
        repo.savePricing(1, new TheaterPolicy.RoomPricing(BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN));
        assertEquals(0, BigDecimal.TEN.compareTo(repo.pricing(1).weekendVip()));
        assertEquals(70000, db.queryForObject("SELECT gia_co_ban FROM SUAT_CHIEU", Integer.class));
        assertNull(repo.policy().lateCheckInMinutes());
        repo.savePolicy(new TheaterPolicy.Settings(15));
        assertEquals(15, repo.policy().lateCheckInMinutes());
        repo.savePolicy(new TheaterPolicy.Settings(null));
        assertNull(repo.policy().lateCheckInMinutes());
        db.update("DELETE FROM PHONG_CHIEU WHERE phong_id=1");
        assertNull(repo.pricing(1));
    }
}
