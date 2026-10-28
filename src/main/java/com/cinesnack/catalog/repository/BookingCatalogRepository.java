package com.cinesnack.catalog.repository;

import com.cinesnack.common.support.Sql;
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
        var movieIds = truyVan.query(
            "SELECT phim_id FROM SUAT_CHIEU WHERE suat_chieu_id = ?",
            (dong, viTri) -> dong.getLong("phim_id"),
            maSo
        );
        if (movieIds.isEmpty()) return null;

        long movieId = movieIds.get(0);
        var activeMovies = truyVan.query(
            "SELECT phim_id FROM PHIM WITH (HOLDLOCK) WHERE phim_id = ? AND trang_thai = 'DANG_CHIEU'",
            (dong, viTri) -> dong.getLong("phim_id"),
            movieId
        );
        if (activeMovies.isEmpty()) return null;

        var shows = truyVan.query(
            """
            SELECT suat_chieu_id, phong_id, gia_co_ban, bat_dau, trang_thai
            FROM SUAT_CHIEU WITH (UPDLOCK, HOLDLOCK)
            WHERE suat_chieu_id = ? AND phim_id = ?
              AND EXISTS(SELECT 1 FROM PHONG_CHIEU r WHERE r.phong_id=SUAT_CHIEU.phong_id AND r.trang_thai='HOAT_DONG')
            """,
            (dong, viTri) ->
                new Show(
                    dong.getLong("suat_chieu_id"),
                    dong.getLong("phong_id"),
                    dong.getBigDecimal("gia_co_ban"),
                    Sql.docThoiGian(dong, "bat_dau"),
                    dong.getString("trang_thai")
                ),
            maSo,
            movieId
        );
        return shows.isEmpty() ? null : shows.get(0);
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
