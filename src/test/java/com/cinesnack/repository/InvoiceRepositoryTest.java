package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class InvoiceRepositoryTest {

    @Test
    void phatHanhHaiLoaiVaDongBangNoiDung() {
        var db = new JdbcTemplate(
            new DriverManagerDataSource("jdbc:h2:mem:invoice_split;DB_CLOSE_DELAY=-1", "sa", "")
        );
        db.execute("DROP ALL OBJECTS");
        db.execute(
            "CREATE TABLE HOA_DON_VE(hoa_don_ve_id BIGINT PRIMARY KEY, don_hang_id BIGINT, ma_hoa_don VARCHAR(30), email_nhan VARCHAR(150), tong_tien DECIMAL(12,2), trang_thai VARCHAR(20), phat_hanh_luc TIMESTAMP)"
        );
        db.execute(
            "CREATE TABLE HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id BIGINT PRIMARY KEY, don_hang_id BIGINT, ma_hoa_don VARCHAR(30), email_nhan VARCHAR(150), tong_tien DECIMAL(12,2), trang_thai VARCHAR(20), phat_hanh_luc TIMESTAMP)"
        );
        db.execute("CREATE TABLE PHIM(phim_id BIGINT PRIMARY KEY, ten_phim VARCHAR(200))");
        db.execute("CREATE TABLE PHONG_CHIEU(phong_id BIGINT PRIMARY KEY, ten_phong VARCHAR(50))");
        db.execute("CREATE TABLE GHE(ghe_id BIGINT PRIMARY KEY, ma_ghe VARCHAR(10))");
        db.execute(
            "CREATE TABLE SUAT_CHIEU(suat_chieu_id BIGINT PRIMARY KEY, phim_id BIGINT, phong_id BIGINT, bat_dau TIMESTAMP)"
        );
        db.execute(
            "CREATE TABLE VE(ve_id BIGINT PRIMARY KEY, hoa_don_ve_id BIGINT, suat_chieu_id BIGINT, ghe_id BIGINT, don_gia DECIMAL(12,2), mo_ta_luc_mua VARCHAR(400))"
        );
        db.execute(
            "CREATE TABLE CHI_TIET_HOA_DON_BONG_NUOC(chi_tiet_id BIGINT PRIMARY KEY, hoa_don_bong_nuoc_id BIGINT, combo_id BIGINT, ten_luc_mua VARCHAR(100), so_luong INT, don_gia DECIMAL(12,2))"
        );
        db.update("INSERT INTO PHIM VALUES(1, 'Phim ban dau')");
        db.update("INSERT INTO PHONG_CHIEU VALUES(1, 'Phong 1')");
        db.update("INSERT INTO GHE VALUES(1, 'A1')");
        db.update("INSERT INTO SUAT_CHIEU VALUES(1,1,1, TIMESTAMP '2026-10-14 13:00:00')");
        db.update("INSERT INTO HOA_DON_VE VALUES(1,1,'HDV1',null,80000,'CHO_THANH_TOAN',null)");
        db.update(
            "INSERT INTO HOA_DON_BONG_NUOC VALUES(1,1,'HDB1',null,59000,'CHO_THANH_TOAN',null)"
        );
        db.update("INSERT INTO VE VALUES(1,1,1,1,80000,null)");
        db.update("INSERT INTO CHI_TIET_HOA_DON_BONG_NUOC VALUES(1,1,1,'Combo ban dau',1,59000)");
        var kho = new InvoiceRepository(db);
        var luc = LocalDateTime.of(2026, 10, 14, 12, 0);
        kho.taoHoaDon(
            new Order(
                1,
                1L,
                "DH1",
                "khach@example.com",
                new BigDecimal("139000"),
                "CHO_THANH_TOAN",
                luc.plusMinutes(15),
                luc
            ),
            luc
        );
        var ds = kho.timTheoDonHang(1);
        assertEquals(2, ds.size());
        assertEquals("VE", ds.get(0).loai());
        assertEquals("BONG_NUOC", ds.get(1).loai());
        BigDecimal tongTien = BigDecimal.ZERO;
        for (var hoaDon : ds) {
            tongTien = tongTien.add(java.util.Objects.requireNonNull(hoaDon.tongTien()));
        }
        assertEquals(0, tongTien.compareTo(new BigDecimal("139000")));
        db.update("UPDATE PHIM SET ten_phim='Phim doi ten'");
        db.update("UPDATE GHE SET ma_ghe='A2'");
        assertEquals(ds, kho.timTheoDonHang(1));
    }
}
