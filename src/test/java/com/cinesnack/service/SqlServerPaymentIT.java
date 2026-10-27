package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.dto.AdminCatalog;
import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.repository.*;
import com.cinesnack.support.Sql;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.*;

/** Kiểm thử opt-in trên CineSnack hiện có; Spring rollback mỗi test. */
@SpringJUnitConfig(SqlServerPaymentIT.Config.class)
@EnabledIfEnvironmentVariable(named = "CINESNACK_TEST_DB_URL", matches = ".+")
@Transactional
class SqlServerPaymentIT {

    @Configuration
    @EnableTransactionManagement
    @Import({
        OrderService.class,
        PaymentService.class,
        InvoiceService.class,
        UserService.class,
        OrderRepository.class,
        BookingCatalogRepository.class,
        PaymentRepository.class,
        InvoiceRepository.class,
        UserRepository.class,
        AdminCatalogRepository.class,
        AdminCatalogService.class,
    })
    static class Config {

        @Bean
        DataSource nguonDuLieu() {
            return new DriverManagerDataSource(
                System.getenv("CINESNACK_TEST_DB_URL"),
                System.getenv("CINESNACK_TEST_DB_USER"),
                System.getenv("CINESNACK_TEST_DB_PASSWORD")
            );
        }

        @Bean
        JdbcTemplate truyVan(DataSource nguonDuLieu) {
            return new JdbcTemplate(nguonDuLieu);
        }

        @Bean
        DataSourceTransactionManager boQuanLyGiaoDich(DataSource nguonDuLieu) {
            return new DataSourceTransactionManager(nguonDuLieu);
        }

        @Bean
        Clock dongHo() {
            return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    @Autowired
    JdbcTemplate truyVan;

    @Autowired
    OrderService dichVuDonHang;

    @Autowired
    PaymentService dichVuThanhToan;

    @Autowired
    InvoiceService dichVuHoaDon;

    @Autowired
    Clock dongHo;

    @Autowired
    AdminCatalogService quanLy;

    @BeforeEach
    void dungDatabaseVaTuyChonSqlServer() {
        assertEquals("CineSnack", truyVan.queryForObject("SELECT DB_NAME()", String.class));
        truyVan.execute(
            "SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET QUOTED_IDENTIFIER ON; SET NUMERIC_ROUNDABORT OFF;"
        );
    }

    @Test
    void adminGuestStaffCyclePreservesInvoicesAndProtectsHistory() {
        String suffix = UUID.randomUUID().toString();
        String admin = "admin-" + suffix + "@example.com",
            staff = "staff-" + suffix + "@example.com";
        String guestEmail = "guest-" + suffix + "@example.com";
        taoNguoiDung(admin, "QUAN_TRI");
        taoNguoiDung(staff, "NHAN_VIEN");
        taoNguoiDung(guestEmail, "KHACH_HANG");
        long room = Sql.themVaLayMa(
            truyVan,
            "INSERT INTO PHONG_CHIEU(ten_phong,loai_phong,trang_thai) OUTPUT INSERTED.phong_id VALUES(?,'2D','HOAT_DONG')",
            "IT-" + suffix
        );
        long seat = Sql.themVaLayMa(
            truyVan,
            "INSERT INTO GHE(phong_id,ma_ghe,loai_ghe,trang_thai) OUTPUT INSERTED.ghe_id VALUES(?,'A1','THUONG','HOAT_DONG')",
            room
        );
        long product = Sql.themVaLayMa(
            truyVan,
            "INSERT INTO SAN_PHAM(ten_san_pham,loai_san_pham,gia_ban,trang_thai) OUTPUT INSERTED.san_pham_id VALUES(N'Bắp IT','BONG',59000,'HOAT_DONG')"
        );
        var movieRequest = new AdminCatalog.MovieRequest(
            "Phim IT " + suffix,
            "Hành động",
            90,
            "T13",
            "",
            "DANG_CHIEU"
        );
        var movie = quanLy.saveMovie(null, movieRequest, admin);
        var startsAt = OffsetDateTime.now(dongHo).plusHours(3).withNano(0);
        var showRequest = new AdminCatalog.ShowRequest(
            movie.maSo(),
            room,
            startsAt,
            new BigDecimal("80000"),
            "SAP_CHIEU"
        );
        var show = quanLy.saveShow(null, showRequest, admin);
        assertEquals(startsAt.plusMinutes(90), show.ketThuc());
        show = quanLy.saveShow(
            show.maSo(),
            new AdminCatalog.ShowRequest(
                movie.maSo(),
                room,
                startsAt,
                new BigDecimal("90000"),
                "SAP_CHIEU"
            ),
            admin
        );
        assertEquals(0, show.gia().compareTo(new BigDecimal("90000")));
        var order = dichVuDonHang.taoDonKhach(
            new CreateOrderRequest(
                guestEmail,
                show.maSo(),
                List.of(seat),
                List.of(new CreateOrderRequest.SnackChoice(product, 1)),
                List.of()
            )
        );
        assertNull(order.maNguoiDung());
        assertEquals(0, order.tongTien().compareTo(new BigDecimal("149000")));
        var ownedCodes = Set.of(order.ma());
        var details = dichVuDonHang.layChiTietKhach(order.ma(), ownedCodes);
        assertEquals(movie.ten(), details.dsVe().get(0).tenPhim());
        assertEquals(0, truyVan.queryForObject(
            "SELECT (SELECT COUNT(*) FROM HOA_DON_VE WHERE don_hang_id=?) + (SELECT COUNT(*) FROM HOA_DON_BONG_NUOC WHERE don_hang_id=?)",
            Integer.class, order.maSo(), order.maSo()));
        assertTrue(
            dichVuDonHang
                .layDanhSachQuanTri(staff, guestEmail, "", 0, 20)
                .stream()
                .anyMatch(o -> o.ma().equals(order.ma()))
        );
        assertEquals(
            403,
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                dichVuDonHang.layChiTiet(order.ma(), guestEmail)
            )
                .getStatusCode()
                .value()
        );
        assertEquals(
            403,
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                dichVuDonHang.layChiTietKhach(order.ma(), Set.of())
            )
                .getStatusCode()
                .value()
        );
        long showId = show.maSo();
        assertEquals(
            409,
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                quanLy.deleteShow(showId, admin)
            )
                .getStatusCode()
                .value()
        );
        var receipt = dichVuThanhToan.thanhToanTaiQuay(order.ma(), new BigDecimal("150000"), staff);
        assertEquals(0, receipt.tienThua().compareTo(new BigDecimal("1000")));
        assertEquals(
            receipt.giaoDich().maSo(),
            dichVuThanhToan
                .thanhToanTaiQuay(order.ma(), new BigDecimal("150000"), staff)
                .giaoDich()
                .maSo()
        );
        var invoices = dichVuHoaDon.layHoaDonKhach(order.ma(), ownedCodes);
        assertEquals(2, invoices.size());
        quanLy.deleteMovie(movie.maSo(), admin);
        assertEquals(
            "NGUNG_CHIEU",
            quanLy
                .movies(admin, 0, 100)
                .stream()
                .filter(m -> m.maSo() == movie.maSo())
                .findFirst()
                .orElseThrow()
                .trangThai()
        );
        assertEquals(invoices, dichVuHoaDon.layHoaDonKhach(order.ma(), ownedCodes));
        var unused = quanLy.saveMovie(null, movieRequest, admin);
        var unusedShow = quanLy.saveShow(
            null,
            new AdminCatalog.ShowRequest(
                unused.maSo(),
                room,
                startsAt.plusHours(3),
                new BigDecimal("75000"),
                "SAP_CHIEU"
            ),
            admin
        );
        quanLy.deleteShow(unusedShow.maSo(), admin);
        quanLy.deleteMovie(unused.maSo(), admin);
        assertTrue(
            quanLy
                .movies(admin, 0, 100)
                .stream()
                .noneMatch(m -> m.maSo() == unused.maSo())
        );
    }

    @Test
    void thuTienMotLanVaGiuNguyenHoaDon() {
        String hauTo = UUID.randomUUID().toString();
        String khachHang = "customer-" + hauTo + "@example.com";
        String nhanVien = "staff-" + hauTo + "@example.com";
        taoNguoiDung(khachHang, "KHACH_HANG");
        taoNguoiDung(nhanVien, "NHAN_VIEN");
        long phong = Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO PHONG_CHIEU(ten_phong, loai_phong, trang_thai)
            OUTPUT INSERTED.phong_id VALUES (?, '2D', 'HOAT_DONG')
            """,
            "IT-" + hauTo
        );
        long phim = Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO PHIM(ten_phim, the_loai, thoi_luong_phut, phan_loai_do_tuoi, trang_thai)
            OUTPUT INSERTED.phim_id VALUES (N'Phim kiểm thử', N'Hành động', 90, 'T13', 'DANG_CHIEU')
            """
        );
        LocalDateTime batDau = LocalDateTime.now(dongHo).plusHours(1).withNano(0);
        long suatChieu = Sql.themVaLayMa(
            truyVan,
            """
            DECLARE @ids TABLE(id BIGINT);
            INSERT INTO SUAT_CHIEU(phim_id, phong_id, bat_dau, ket_thuc, gia_co_ban, trang_thai)
            OUTPUT INSERTED.suat_chieu_id INTO @ids VALUES (?, ?, ?, ?, 80000, 'SAP_CHIEU'); SELECT id FROM @ids
            """,
            phim,
            phong,
            batDau,
            batDau.plusMinutes(90)
        );
        long ghe = Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO GHE(phong_id, ma_ghe, loai_ghe, trang_thai)
            OUTPUT INSERTED.ghe_id VALUES (?, 'A1', 'THUONG', 'HOAT_DONG')
            """,
            phong
        );
        long sanPham = Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO SAN_PHAM(ten_san_pham, loai_san_pham, gia_ban, trang_thai)
            OUTPUT INSERTED.san_pham_id VALUES (N'Bắp kiểm thử', 'BONG', 59000, 'HOAT_DONG')
            """
        );
        var donHang = dichVuDonHang.taoDonHang(
            new CreateOrderRequest(
                khachHang,
                suatChieu,
                List.of(ghe),
                List.of(new CreateOrderRequest.SnackChoice(sanPham, 1)),
                List.of()
            ),
            khachHang
        );
        var lanDau = dichVuThanhToan.thanhToanTaiQuay(
            donHang.ma(),
            new BigDecimal("150000"),
            nhanVien
        );
        var lanLapLai = dichVuThanhToan.thanhToanTaiQuay(
            donHang.ma(),
            new BigDecimal("150000"),
            nhanVien
        );
        assertEquals(lanDau.giaoDich().maSo(), lanLapLai.giaoDich().maSo());
        assertEquals(0, lanDau.tienThua().compareTo(new BigDecimal("11000")));
        var hoaDon = dichVuHoaDon.layHoaDon(donHang.ma(), khachHang);
        assertEquals(2, hoaDon.size());
        truyVan.update(
            "UPDATE SAN_PHAM SET gia_ban = 99999, ten_san_pham = N'Đổi tên' WHERE san_pham_id = ?",
            sanPham
        );
        truyVan.update("UPDATE PHIM SET ten_phim = N'Đổi tên phim' WHERE phim_id = ?", phim);
        assertEquals(hoaDon, dichVuHoaDon.layHoaDon(donHang.ma(), khachHang));
        assertEquals(
            1,
            truyVan.queryForObject(
                "SELECT COUNT(*) FROM THANH_TOAN WHERE don_hang_id = ?",
                Integer.class,
                donHang.maSo()
            )
        );
        assertEquals(
            "CHO_NHAN",
            truyVan.queryForObject(
                "SELECT c.trang_thai_phuc_vu FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id WHERE h.don_hang_id = ?",
                String.class,
                donHang.maSo()
            )
        );
    }

    private void taoNguoiDung(String thuDienTu, String vaiTro) {
        truyVan.update(
            """
            INSERT INTO NGUOI_DUNG(ho_ten, email, mat_khau_hash, vai_tro, trang_thai)
            VALUES (N'Tài khoản kiểm thử', ?, 'unused-test-hash', ?, 'HOAT_DONG')
            """,
            thuDienTu,
            vaiTro
        );
    }
}
