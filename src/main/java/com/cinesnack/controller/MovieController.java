
package com.cinesnack.controller;

import com.cinesnack.entity.Movie;
import com.cinesnack.service.MovieService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }
    
@PostMapping
public String addMovie(@RequestBody Movie movie) {
    int result = movieService.addMovie(movie);

    if (result > 0) {
        return "Them phim thanh cong!";
    }

    return "Them phim that bai!";
}
@PutMapping("/{id}")
public ResponseEntity<String> updateMovie(
        @PathVariable Long id,
        @RequestBody Movie movie) {

    int result = movieService.updateMovie(id, movie);

    if (result > 0) {
        return ResponseEntity.ok("Cap nhat phim thanh cong!");
    }

    return ResponseEntity.status(404)
            .body("Khong tim thay phim!");
}

}
