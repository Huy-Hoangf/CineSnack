package com.cinesnack.repository;

import com.cinesnack.dto.Catalog;
import com.cinesnack.support.Sql;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private final JdbcTemplate truyVan;

    public CatalogRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public List<Catalog.Movie> layPhim(String tuTim, String trangThai, int trang, int soLuong) {
        return truyVan.query(
            """
            SELECT * FROM PHIM WHERE trang_thai IN ('DANG_CHIEU','SAP_CHIEU')
            AND (?='' OR trang_thai=?) AND (ten_phim LIKE ? OR the_loai LIKE ?)
            ORDER BY phim_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """,
            (d, i) ->
                new Catalog.Movie(
                    d.getLong("phim_id"),
                    d.getString("ten_phim"),
                    d.getString("the_loai"),
                    d.getInt("thoi_luong_phut"),
                    d.getString("phan_loai_do_tuoi"),
                    d.getString("poster_url"),
                    d.getString("trang_thai")
                ),
            trangThai,
            trangThai,
            "%" + tuTim + "%",
            "%" + tuTim + "%",
            (long) trang * soLuong,
            soLuong
        );
    }

    public Catalog.Movie timPhim(long maPhim) {
        var ds = truyVan.query(
            "SELECT * FROM PHIM WHERE phim_id=? AND trang_thai IN ('DANG_CHIEU','SAP_CHIEU')",
            (d, i) ->
                new Catalog.Movie(
                    d.getLong("phim_id"),
                    d.getString("ten_phim"),
                    d.getString("the_loai"),
                    d.getInt("thoi_luong_phut"),
                    d.getString("phan_loai_do_tuoi"),
                    d.getString("poster_url"),
                    d.getString("trang_thai")
                ),
            maPhim
        );
        return ds.isEmpty() ? null : ds.get(0);
    }

    public List<Catalog.Show> laySuat(Long maPhim, LocalDateTime luc) {
        return truyVan.query(
            """
            SELECT TOP(300) s.*,p.ten_phim,r.ten_phong FROM SUAT_CHIEU s
            JOIN PHIM p ON p.phim_id=s.phim_id JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id
            WHERE s.trang_thai='SAP_CHIEU' AND s.bat_dau>? AND r.trang_thai='HOAT_DONG'
              AND p.trang_thai='DANG_CHIEU' AND (? IS NULL OR s.phim_id=?) ORDER BY s.bat_dau,s.suat_chieu_id
            """,
            (d, i) ->
                new Catalog.Show(
                    d.getLong("suat_chieu_id"),
                    d.getLong("phim_id"),
                    d.getString("ten_phim"),
                    d.getLong("phong_id"),
                    d.getString("ten_phong"),
                    Sql.docThoiGian(d, "bat_dau").atOffset(ZoneOffset.ofHours(7)),
                    Sql.docThoiGian(d, "ket_thuc").atOffset(ZoneOffset.ofHours(7)),
                    d.getBigDecimal("gia_co_ban"),
                    d.getString("trang_thai")
                ),
            luc,
            maPhim,
            maPhim
        );
    }

    public List<Catalog.Seat> layGhe(long maSuat, LocalDateTime luc) {
        return truyVan.query(
            """
            SELECT g.ghe_id,g.ma_ghe,g.loai_ghe,s.gia_co_ban,
              CASE WHEN g.trang_thai='HOAT_DONG' AND r.trang_thai='HOAT_DONG' AND p.trang_thai='DANG_CHIEU' AND s.trang_thai='SAP_CHIEU' AND s.bat_dau>?
                AND NOT EXISTS(SELECT 1 FROM VE v WHERE v.suat_chieu_id=s.suat_chieu_id AND v.ghe_id=g.ghe_id
                  AND v.trang_thai IN('CHO_THANH_TOAN','DA_THANH_TOAN','DA_SOAT')) THEN 1 ELSE 0 END con_trong
            FROM SUAT_CHIEU s JOIN GHE g ON g.phong_id=s.phong_id JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id JOIN PHIM p ON p.phim_id=s.phim_id
            WHERE s.suat_chieu_id=? ORDER BY g.ghe_id
            """,
            (d, i) ->
                new Catalog.Seat(
                    d.getLong("ghe_id"),
                    d.getString("ma_ghe"),
                    d.getString("loai_ghe"),
                    d.getInt("con_trong") == 1,
                    d.getBigDecimal("gia_co_ban")
                ),
            luc,
            maSuat
        );
    }

    public Catalog.Snacks layBapNuoc() {
        var sanPham = truyVan.query(
            "SELECT TOP(200) san_pham_id,ten_san_pham,gia_ban FROM SAN_PHAM WHERE trang_thai='HOAT_DONG' ORDER BY san_pham_id",
            (d, i) ->
                new Catalog.Food(
                    d.getLong("san_pham_id"),
                    d.getString("ten_san_pham"),
                    d.getBigDecimal("gia_ban")
                )
        );
        var combo = truyVan.query(
            "SELECT TOP(200) combo_id,ten_combo,gia_ban FROM COMBO WHERE trang_thai='HOAT_DONG' ORDER BY combo_id",
            (d, i) ->
                new Catalog.Food(
                    d.getLong("combo_id"),
                    d.getString("ten_combo"),
                    d.getBigDecimal("gia_ban")
                )
        );
        return new Catalog.Snacks(sanPham, combo);
    }
}
