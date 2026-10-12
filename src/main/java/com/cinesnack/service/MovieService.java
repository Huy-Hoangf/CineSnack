
package com.cinesnack.service;

import com.cinesnack.entity.Movie;
import com.cinesnack.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }
    
public int addMovie(Movie movie) {
    if (movie.getTenPhim() == null
            || movie.getTenPhim().isBlank()) {
        throw new IllegalArgumentException(
            "Ten phim khong duoc de trong"
        );
    }

    if (movie.getTheLoai() == null
            || movie.getTheLoai().isBlank()) {
        throw new IllegalArgumentException(
            "The loai khong duoc de trong"
        );
    }

    if (movie.getThoiLuongPhut() == null
            || movie.getThoiLuongPhut() <= 0) {
        throw new IllegalArgumentException(
            "Thoi luong phim phai lon hon 0"
        );
    }

    return movieRepository.save(movie);
}

public int updateMovie(Long id, Movie movie) {
    if (id == null || id <= 0) {
        throw new IllegalArgumentException("ID phim khong hop le");
    }

    if (movie.getTenPhim() == null
            || movie.getTenPhim().isBlank()) {
        throw new IllegalArgumentException(
            "Ten phim khong duoc de trong"
        );
    }

    if (movie.getTheLoai() == null
            || movie.getTheLoai().isBlank()) {
        throw new IllegalArgumentException(
            "The loai khong duoc de trong"
        );
    }

    if (movie.getThoiLuongPhut() == null
            || movie.getThoiLuongPhut() <= 0) {
        throw new IllegalArgumentException(
            "Thoi luong phim phai lon hon 0"
        );
    }

    if (movie.getPhanLoaiDoTuoi() == null
            || movie.getPhanLoaiDoTuoi().isBlank()
            || movie.getTrangThai() == null
            || movie.getTrangThai().isBlank()) {
        throw new IllegalArgumentException(
            "Thieu thong tin bat buoc"
        );
    }

    movie.setPhimId(id);
    return movieRepository.update(movie);
}


}
