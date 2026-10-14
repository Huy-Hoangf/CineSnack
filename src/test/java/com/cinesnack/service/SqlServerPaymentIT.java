package com.cinesnack.service;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.repository.*;
import com.cinesnack.support.Sql;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.*;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ chạy khi người dùng cấu hình riêng database kiểm thử SQL Server. */
@SpringJUnitConfig(SqlServerPaymentIT.Config.class)
@EnabledIfEnvironmentVariable(named = "CINESNACK_TEST_DB_URL", matches = ".+")
@Transactional
class SqlServerPaymentIT {
    @Configuration
    @EnableTransactionManagement
    @Import({OrderService.class, PaymentService.class, InvoiceService.class, UserService.class,
            OrderRepository.class, BookingCatalogRepository.class, PaymentRepository.class,
            InvoiceRepository.class, UserRepository.class})
    static class Config {
        @Bean DataSource nguonDuLieu() {
            return new DriverManagerDataSource(System.getenv("CINESNACK_TEST_DB_URL"),
                    System.getenv("CINESNACK_TEST_DB_USER"), System.getenv("CINESNACK_TEST_DB_PASSWORD"));
        }
        @Bean JdbcTemplate truyVan(DataSource nguonDuLieu) { return new JdbcTemplate(nguonDuLieu); }
        @Bean DataSourceTransactionManager boQuanLyGiaoDich(DataSource nguonDuLieu) { return new DataSourceTransactionManager(nguonDuLieu); }
        @Bean Clock dongHo() { return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")); }
    }

    @Autowired JdbcTemplate truyVan;
    @Autowired OrderService dichVuDonHang;
    @Autowired PaymentService dichVuThanhToan;
    @Autowired InvoiceService dichVuHoaDon;
    @Autowired Clock dongHo;

    @Test
    void thuTienMotLanVaGiuNguyenHoaDon() {
        String hauTo = UUID.randomUUID().toString();
        String khachHang = "customer-" + hauTo + "@example.com";
        String nhanVien = "staff-" + hauTo + "@example.com";
        taoNguoiDung(khachHang, "KHACH_HANG");
        taoNguoiDung(nhanVien, "NHAN_VIEN");
        long phong = Sql.themVaLayMa(truyVan, """
                INSERT INTO PHONG_CHIEU(ten_phong, loai_phong, trang_thai)
                OUTPUT INSERTED.phong_id VALUES (?, '2D', 'HOAT_DONG')
                """, "IT-" + hauTo);
        long phim = Sql.themVaLayMa(truyVan, """
                INSERT INTO PHIM(ten_phim, the_loai, thoi_luong_phut, phan_loai_do_tuoi, trang_thai)
                OUTPUT INSERTED.phim_id VALUES (N'Phim kiểm thử', N'Hành động', 90, 'T13', 'DANG_CHIEU')
                """);
        LocalDateTime batDau = LocalDateTime.now(dongHo).plusHours(1).withNano(0);
        long suatChieu = Sql.themVaLayMa(truyVan, """
                DECLARE @ids TABLE(id BIGINT);
                INSERT INTO SUAT_CHIEU(phim_id, phong_id, bat_dau, ket_thuc, gia_co_ban, trang_thai)
                OUTPUT INSERTED.suat_chieu_id INTO @ids VALUES (?, ?, ?, ?, 80000, 'SAP_CHIEU'); SELECT id FROM @ids
                """, phim, phong, batDau, batDau.plusMinutes(90));
        long ghe = Sql.themVaLayMa(truyVan, """
                INSERT INTO GHE(phong_id, ma_ghe, loai_ghe, trang_thai)
                OUTPUT INSERTED.ghe_id VALUES (?, 'A1', 'THUONG', 'HOAT_DONG')
                """, phong);
        long sanPham = Sql.themVaLayMa(truyVan, """
                INSERT INTO SAN_PHAM(ten_san_pham, loai_san_pham, gia_ban, trang_thai)
                OUTPUT INSERTED.san_pham_id VALUES (N'Bắp kiểm thử', 'BONG', 59000, 'HOAT_DONG')
                """);
        var donHang = dichVuDonHang.taoDonHang(new CreateOrderRequest(khachHang, suatChieu, List.of(ghe),
                List.of(new CreateOrderRequest.SnackChoice(sanPham, 1)), List.of()), khachHang);
        var lanDau = dichVuThanhToan.thanhToanTaiQuay(donHang.ma(), new BigDecimal("150000"), nhanVien);
        var lanLapLai = dichVuThanhToan.thanhToanTaiQuay(donHang.ma(), new BigDecimal("150000"), nhanVien);
        assertEquals(lanDau.giaoDich().maSo(), lanLapLai.giaoDich().maSo());
        assertEquals(0, lanDau.tienThua().compareTo(new BigDecimal("11000")));
        var hoaDon = dichVuHoaDon.layHoaDon(donHang.ma(), khachHang);
        assertEquals(2, hoaDon.size());
        truyVan.update("UPDATE SAN_PHAM SET gia_ban = 99999, ten_san_pham = N'Đổi tên' WHERE san_pham_id = ?", sanPham);
        truyVan.update("UPDATE PHIM SET ten_phim = N'Đổi tên phim' WHERE phim_id = ?", phim);
        assertEquals(hoaDon, dichVuHoaDon.layHoaDon(donHang.ma(), khachHang));
        assertEquals(1, truyVan.queryForObject("SELECT COUNT(*) FROM THANH_TOAN WHERE don_hang_id = ?", Integer.class, donHang.maSo()));
        assertEquals("CHO_NHAN", truyVan.queryForObject("SELECT c.trang_thai_phuc_vu FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id WHERE h.don_hang_id = ?", String.class, donHang.maSo()));
    }

    private void taoNguoiDung(String thuDienTu, String vaiTro) {
        truyVan.update("""
                INSERT INTO NGUOI_DUNG(ho_ten, email, mat_khau_hash, vai_tro, trang_thai)
                VALUES (N'Tài khoản kiểm thử', ?, 'unused-test-hash', ?, 'HOAT_DONG')
                """, thuDienTu, vaiTro);
    }
}
