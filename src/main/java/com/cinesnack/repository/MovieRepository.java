
package com.cinesnack.repository;

import com.cinesnack.entity.Movie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MovieRepository {

    private final JdbcTemplate jdbcTemplate;

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Movie> findAll() {
        String sql = "SELECT * FROM PHIM";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Movie movie = new Movie();

            movie.setPhimId(rs.getLong("phim_id"));
            movie.setTenPhim(rs.getString("ten_phim"));
            movie.setTheLoai(rs.getString("the_loai"));
            movie.setThoiLuongPhut(
                rs.getInt("thoi_luong_phut")
            );
            movie.setPhanLoaiDoTuoi(
                rs.getString("phan_loai_do_tuoi")
            );
            movie.setPosterUrl(rs.getString("poster_url"));
            movie.setTrangThai(rs.getString("trang_thai"));

            return movie;
        });
    }
    
public int save(Movie movie) {
    String sql = """
        INSERT INTO PHIM
        (ten_phim, the_loai, thoi_luong_phut,
         phan_loai_do_tuoi, poster_url, trang_thai)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    return jdbcTemplate.update(
        sql,
        movie.getTenPhim(),
        movie.getTheLoai(),
        movie.getThoiLuongPhut(),
        movie.getPhanLoaiDoTuoi(),
        movie.getPosterUrl(),
        movie.getTrangThai()
    );
}

public int update(Movie movie) {
    String sql = """
        UPDATE PHIM
        SET ten_phim = ?,
            the_loai = ?,
            thoi_luong_phut = ?,
            phan_loai_do_tuoi = ?,
            poster_url = ?,
            trang_thai = ?
        WHERE phim_id = ?
        """;

    return jdbcTemplate.update(
        sql,
        movie.getTenPhim(),
        movie.getTheLoai(),
        movie.getThoiLuongPhut(),
        movie.getPhanLoaiDoTuoi(),
        movie.getPosterUrl(),
        movie.getTrangThai(),
        movie.getPhimId()
    );
}

}
