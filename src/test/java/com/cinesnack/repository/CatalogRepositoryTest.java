package com.cinesnack.repository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CatalogRepositoryTest {
    @Test void chiTraSuatMoBanVaGheTheoTrangThaiVe() {
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalog;MODE=MSSQLServer;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE TABLE PHIM(phim_id BIGINT,ten_phim VARCHAR(200),the_loai VARCHAR(100),thoi_luong_phut INT,phan_loai_do_tuoi VARCHAR(10),poster_url VARCHAR(500),trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT,ten_phong VARCHAR(50),trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT,phim_id BIGINT,phong_id BIGINT,bat_dau TIMESTAMP,ket_thuc TIMESTAMP,gia_co_ban DECIMAL(12,2),trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE GHE(ghe_id BIGINT,phong_id BIGINT,ma_ghe VARCHAR(10),loai_ghe VARCHAR(20),trang_thai VARCHAR(20))");
        db.execute("CREATE TABLE VE(suat_chieu_id BIGINT,ghe_id BIGINT,trang_thai VARCHAR(20))");
        db.update("INSERT INTO PHIM VALUES(1,'Phim','Han dong',90,'T13',NULL,'DANG_CHIEU')");
        db.update("INSERT INTO PHONG_CHIEU VALUES(1,'Phong 1','HOAT_DONG')");
        db.update("INSERT INTO SUAT_CHIEU VALUES(1,1,1,TIMESTAMP '2026-10-14 13:00:00',TIMESTAMP '2026-10-14 14:30:00',80000,'SAP_CHIEU')");
        db.update("INSERT INTO GHE VALUES(11,1,'A1','THUONG','HOAT_DONG'),(12,1,'A2','VIP','HOAT_DONG')");
        db.update("INSERT INTO VE VALUES(1,11,'DA_THANH_TOAN')");
        var kho=new CatalogRepository(db);var luc=LocalDateTime.of(2026,10,14,12,0);
        assertEquals(1,kho.layPhim("","DANG_CHIEU",0,20).size());
        assertEquals(0,kho.layPhim("khongcophim","",0,20).size());
        assertEquals(1,kho.laySuat(1L,luc).size());assertEquals("+07:00",kho.laySuat(1L,luc).get(0).batDau().getOffset().toString());
        var ghe=kho.layGhe(1,luc);assertFalse(ghe.get(0).conTrong());assertTrue(ghe.get(1).conTrong());
        assertEquals(ghe.get(0).gia(),ghe.get(1).gia());
        assertEquals(0,kho.laySuat(1L,luc.plusHours(1)).size());
        db.update("UPDATE VE SET trang_thai='HUY'");assertTrue(kho.layGhe(1,luc).get(0).conTrong());
        db.update("UPDATE PHIM SET trang_thai='NGUNG_CHIEU'");
        assertEquals(0,kho.laySuat(1L,luc).size());
        assertFalse(kho.layGhe(1,luc).stream().anyMatch(v -> v.conTrong()));
    }
}
