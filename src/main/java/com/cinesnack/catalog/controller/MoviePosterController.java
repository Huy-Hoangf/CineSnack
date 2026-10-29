package com.cinesnack.catalog.controller;

import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.service.MoviePosterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/admin/movies")
public class MoviePosterController {
    private final MoviePosterService service;
    public MoviePosterController(MoviePosterService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Catalog.Movie create(@Valid @RequestPart("movie") AdminCatalog.MovieRequest movie,
        @RequestPart(value = "poster", required = false) MultipartFile poster, Principal principal) {
        return service.save(null, movie, poster, principal.getName());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Catalog.Movie update(@PathVariable @Positive long id,
        @Valid @RequestPart("movie") AdminCatalog.MovieRequest movie,
        @RequestPart(value = "poster", required = false) MultipartFile poster, Principal principal) {
        return service.save(id, movie, poster, principal.getName());
    }
}
