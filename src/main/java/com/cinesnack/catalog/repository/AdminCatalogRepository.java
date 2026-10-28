package com.cinesnack.catalog.repository;

import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.common.support.Sql;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminCatalogRepository {

    private final JdbcTemplate jdbc;
    private static final String SHOW_SELECT = "SELECT s.*,p.ten_phim,r.ten_phong FROM SUAT_CHIEU s JOIN PHIM p ON p.phim_id=s.phim_id JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id ";

    public AdminCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Catalog.Movie> movies(int page, int size) {
        return jdbc.query(
                "SELECT * FROM PHIM ORDER BY phim_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (r, i) -> movie(r),
                (long) page * size,
                size);
    }

    public Catalog.Movie movie(long id) {
        var rows = jdbc.query("SELECT * FROM PHIM WHERE phim_id=?", (r, i) -> movie(r), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Catalog.Movie lockMovie(long id) {
        var rows = jdbc.query(
                "SELECT * FROM PHIM WITH (XLOCK,HOLDLOCK) WHERE phim_id=?",
                (r, i) -> movie(r),
                id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long insertMovie(AdminCatalog.MovieRequest m) {
        return Sql.themVaLayMa(
                jdbc,
                "INSERT INTO PHIM(ten_phim,the_loai,thoi_luong_phut,phan_loai_do_tuoi,poster_url,trang_thai) OUTPUT INSERTED.phim_id VALUES(?,?,?,?,?,?)",
                m.name().trim(),
                m.genre().trim(),
                m.duration(),
                m.ageRating(),
                m.posterUrl(),
                m.status());
    }

    public void updateMovie(long id, AdminCatalog.MovieRequest m) {
        jdbc.update(
                "UPDATE PHIM SET ten_phim=?,the_loai=?,thoi_luong_phut=?,phan_loai_do_tuoi=?,poster_url=?,trang_thai=? WHERE phim_id=?",
                m.name().trim(),
                m.genre().trim(),
                m.duration(),
                m.ageRating(),
                m.posterUrl(),
                m.status(),
                id);
    }

    public boolean movieHasShows(long id) {
        return count("SELECT COUNT(*) FROM SUAT_CHIEU WHERE phim_id=?", id);
    }

    public boolean movieHasTickets(long id) {
        return count(
                "SELECT COUNT(*) FROM VE v JOIN SUAT_CHIEU s ON s.suat_chieu_id=v.suat_chieu_id WHERE s.phim_id=?",
                id);
    }

    public boolean movieHasPendingTickets(long id) {
        return count(
                "SELECT COUNT(*) FROM VE v JOIN SUAT_CHIEU s ON s.suat_chieu_id=v.suat_chieu_id WHERE s.phim_id=? AND v.trang_thai='CHO_THANH_TOAN'",
                id);
    }

    public void archiveMovie(long id) {
        jdbc.update("UPDATE PHIM SET trang_thai='NGUNG_CHIEU' WHERE phim_id=?", id);
    }

    public void deleteMovie(long id) {
        jdbc.update("DELETE FROM PHIM WHERE phim_id=?", id);
    }

    public List<Catalog.Show> shows(int page, int size) {
        return jdbc.query(
                SHOW_SELECT +
                        "ORDER BY s.bat_dau DESC,s.suat_chieu_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (r, i) -> show(r),
                (long) page * size,
                size);
    }

    public Catalog.Show show(long id) {
        var rows = jdbc.query(SHOW_SELECT + "WHERE s.suat_chieu_id=?", (r, i) -> show(r), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Catalog.Show lockShow(long id) {
        var rows = jdbc.query(
                "SELECT suat_chieu_id FROM SUAT_CHIEU WITH (UPDLOCK,HOLDLOCK) WHERE suat_chieu_id=?",
                (r, i) -> r.getLong(1),
                id);
        return rows.isEmpty() ? null : show(id);
    }

    // Serialize schedule writes per room; the DB overlap trigger is a second guard.
    public boolean lockRoom(long id) {
        var rows = jdbc.query(
                "SELECT trang_thai FROM PHONG_CHIEU WITH (UPDLOCK,HOLDLOCK) WHERE phong_id=?",
                (r, i) -> r.getString(1),
                id);
        return !rows.isEmpty() && "HOAT_DONG".equals(rows.get(0));
    }

    public boolean overlaps(long roomId, LocalDateTime start, LocalDateTime end, long excludedId) {
        return count(
                "SELECT COUNT(*) FROM SUAT_CHIEU WHERE phong_id=? AND suat_chieu_id<>? AND trang_thai<>'HUY' AND bat_dau<? AND ket_thuc>?",
                roomId,
                excludedId,
                end,
                start);
    }

    public boolean showHasTickets(long id) {
        return count("SELECT COUNT(*) FROM VE WHERE suat_chieu_id=?", id);
    }

    public long insertShow(AdminCatalog.ShowRequest s, LocalDateTime start, LocalDateTime end) {
        // INSERT OUTPUT INTO is required because SUAT_CHIEU has an enabled trigger.
        return Sql.themVaLayMa(
                jdbc,
                "SET NOCOUNT ON; DECLARE @ids TABLE(id BIGINT); INSERT INTO SUAT_CHIEU(phim_id,phong_id,bat_dau,ket_thuc,gia_co_ban,trang_thai) OUTPUT INSERTED.suat_chieu_id INTO @ids VALUES(?,?,?,?,?,?); SELECT id FROM @ids; SET NOCOUNT OFF;",
                s.movieId(),
                s.roomId(),
                start,
                end,
                s.price(),
                s.status());
    }

    public void updateShow(
            long id,
            AdminCatalog.ShowRequest s,
            LocalDateTime start,
            LocalDateTime end) {
        jdbc.update(
                "UPDATE SUAT_CHIEU SET phim_id=?,phong_id=?,bat_dau=?,ket_thuc=?,gia_co_ban=?,trang_thai=? WHERE suat_chieu_id=?",
                s.movieId(),
                s.roomId(),
                start,
                end,
                s.price(),
                s.status(),
                id);
    }

    public void deleteShow(long id) {
        jdbc.update("DELETE FROM SUAT_CHIEU WHERE suat_chieu_id=?", id);
    }

    public List<AdminCatalog.Room> rooms() {
        return jdbc.query(
                "SELECT phong_id,ten_phong FROM PHONG_CHIEU WHERE trang_thai='HOAT_DONG' ORDER BY phong_id",
                (r, i) -> new AdminCatalog.Room(r.getLong(1), r.getString(2)));
    }

    private boolean count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args) > 0;
    }

    private Catalog.Movie movie(ResultSet r) throws SQLException {
        return new Catalog.Movie(
                r.getLong("phim_id"),
                r.getString("ten_phim"),
                r.getString("the_loai"),
                r.getInt("thoi_luong_phut"),
                r.getString("phan_loai_do_tuoi"),
                r.getString("poster_url"),
                r.getString("trang_thai"));
    }

    private Catalog.Show show(ResultSet r) throws SQLException {
        return new Catalog.Show(
                r.getLong("suat_chieu_id"),
                r.getLong("phim_id"),
                r.getString("ten_phim"),
                r.getLong("phong_id"),
                r.getString("ten_phong"),
                Sql.docThoiGian(r, "bat_dau").atOffset(ZoneOffset.ofHours(7)),
                Sql.docThoiGian(r, "ket_thuc").atOffset(ZoneOffset.ofHours(7)),
                r.getBigDecimal("gia_co_ban"),
                r.getString("trang_thai"));
    }
}
