package com.cinesnack.repository;

import com.cinesnack.support.Sql;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingCatalogRepository {

    public record Show(
        long maSo,
        long maPhong,
        BigDecimal gia,
        LocalDateTime batDau,
        String trangThai
    ) {}

    public record Seat(long maSo, long maPhong, String trangThai) {}

    public record Food(long maSo, String ten, BigDecimal gia, String trangThai) {}

    private final JdbcTemplate truyVan;

    public BookingCatalogRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public Show timVaKhoaSuatChieu(long maSo) {
        var dsGiaTri = truyVan.query(
            """
            SELECT suat_chieu_id, phong_id, gia_co_ban, bat_dau, trang_thai
            FROM SUAT_CHIEU WITH (UPDLOCK, HOLDLOCK) WHERE suat_chieu_id = ?
              AND EXISTS(SELECT 1 FROM PHONG_CHIEU r WHERE r.phong_id=SUAT_CHIEU.phong_id AND r.trang_thai='HOAT_DONG')
              AND EXISTS(SELECT 1 FROM PHIM p WHERE p.phim_id=SUAT_CHIEU.phim_id AND p.trang_thai='DANG_CHIEU')
            """,
            (dong, viTri) ->
                new Show(
                    dong.getLong("suat_chieu_id"),
                    dong.getLong("phong_id"),
                    dong.getBigDecimal("gia_co_ban"),
                    Sql.docThoiGian(dong, "bat_dau"),
                    dong.getString("trang_thai")
                ),
            maSo
        );
        return dsGiaTri.isEmpty() ? null : dsGiaTri.get(0);
    }

    public Seat timGhe(long maSo) {
        var dsGiaTri = truyVan.query(
            "SELECT ghe_id, phong_id, trang_thai FROM GHE WHERE ghe_id = ?",
            (dong, viTri) ->
                new Seat(
                    dong.getLong("ghe_id"),
                    dong.getLong("phong_id"),
                    dong.getString("trang_thai")
                ),
            maSo
        );
        return dsGiaTri.isEmpty() ? null : dsGiaTri.get(0);
    }

    public Food timSanPham(long maSo) {
        return timMatHang(
            "SELECT san_pham_id AS id, ten_san_pham AS ten, gia_ban, trang_thai FROM SAN_PHAM WHERE san_pham_id = ?",
            maSo
        );
    }

    public Food timCombo(long maSo) {
        return timMatHang(
            "SELECT combo_id AS id, ten_combo AS ten, gia_ban, trang_thai FROM COMBO WHERE combo_id = ?",
            maSo
        );
    }

    private Food timMatHang(String cauLenh, long maSo) {
        var dsGiaTri = truyVan.query(
            cauLenh,
            (dong, viTri) ->
                new Food(
                    dong.getLong("id"),
                    dong.getString("ten"),
                    dong.getBigDecimal("gia_ban"),
                    dong.getString("trang_thai")
                ),
            maSo
        );
        return dsGiaTri.isEmpty() ? null : dsGiaTri.get(0);
    }
}
