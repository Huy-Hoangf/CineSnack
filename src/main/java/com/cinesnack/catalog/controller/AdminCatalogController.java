package com.cinesnack.catalog.controller;

import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.service.AdminCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) {
        this.service = service;
    }

    @GetMapping("/movies")
    public List<Catalog.Movie> movies(
        @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        return service.movies(principal.getName(), page, size);
    }

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public Catalog.Movie createMovie(
        @Valid @RequestBody AdminCatalog.MovieRequest request,
        Principal principal
    ) {
        return service.saveMovie(null, request, principal.getName());
    }

    @PutMapping("/movies/{id}")
    public Catalog.Movie updateMovie(
        @PathVariable @Positive long id,
        @Valid @RequestBody AdminCatalog.MovieRequest request,
        Principal principal
    ) {
        return service.saveMovie(id, request, principal.getName());
    }

    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable @Positive long id, Principal principal) {
        service.deleteMovie(id, principal.getName());
    }

    @GetMapping("/showtimes")
    public List<Catalog.Show> shows(
        @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
        Principal principal
    ) {
        return service.shows(principal.getName(), page, size);
    }

    @PostMapping("/showtimes")
    @ResponseStatus(HttpStatus.CREATED)
    public Catalog.Show createShow(
        @Valid @RequestBody AdminCatalog.ShowRequest request,
        Principal principal
    ) {
        return service.saveShow(null, request, principal.getName());
    }

    @PutMapping("/showtimes/{id}")
    public Catalog.Show updateShow(
        @PathVariable @Positive long id,
        @Valid @RequestBody AdminCatalog.ShowRequest request,
        Principal principal
    ) {
        return service.saveShow(id, request, principal.getName());
    }

    @DeleteMapping("/showtimes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShow(@PathVariable @Positive long id, Principal principal) {
        service.deleteShow(id, principal.getName());
    }

    @GetMapping("/rooms")
    public List<AdminCatalog.Room> rooms(Principal principal) {
        return service.rooms(principal.getName());
    }
}
