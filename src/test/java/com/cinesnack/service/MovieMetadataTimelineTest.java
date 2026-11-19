package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.repository.AdminCatalogRepository;
import com.cinesnack.catalog.service.AdminCatalogService;
import jakarta.validation.Validation;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MovieMetadataTimelineTest {
    private final AdminCatalogRepository repo = mock(AdminCatalogRepository.class);
    private final UserService users = mock(UserService.class);
    private final AdminCatalogService service = new AdminCatalogService(repo, users, Clock.systemUTC(), mock(com.cinesnack.catalog.service.TheaterPolicyService.class));

    private AdminCatalog.MovieRequest edit(String name, String genre, int duration, String rating, String status, String director, String trailer) {
        return new AdminCatalog.MovieRequest(name, genre, "New description", "New actors", duration, rating,
            "/new.png", status, director, trailer);
    }

    @Test
    void trailerMustBeHttpsWithHostAndWithoutCredentials() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String trailer : new String[] {"http://example.com/a", "javascript:alert(1)", "https://user:pass@example.com/a", "https:/nohost", "https://example.com/" + "x".repeat(500)}) {
                assertFalse(validator.validate(edit("Phim", "Hài", 90, "P", "DANG_CHIEU", "Đạo diễn", trailer)).isEmpty(), trailer);
            }
            for (String trailer : new String[] {null, "", "https://example.com/watch?v=1"}) {
                assertTrue(validator.validate(edit("Phim", "Hài", 90, "P", "DANG_CHIEU", null, trailer)).isEmpty());
            }
            assertFalse(validator.validate(edit("Phim", "Hài", 90, "P", "DANG_CHIEU", "x".repeat(201), null)).isEmpty());
        }
    }

    @Test
    void soldMovieAllowsMetadataButProtectsHistoricalAttributes() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(new User(1, "admin", "", "QUAN_TRI", "HOAT_DONG"));
        var old = new Catalog.Movie(1, "Phim", "Hài", "Old", "Old actors", 90, "P", null, "DANG_CHIEU");
        when(repo.lockMovie(1)).thenReturn(old);
        when(repo.movieHasTickets(1)).thenReturn(true);
        var metadata = edit("Phim", "Hài", 90, "P", "DANG_CHIEU", "Đạo diễn", "https://example.com/trailer");
        when(repo.movie(1)).thenReturn(old);
        assertDoesNotThrow(() -> service.saveMovie(1L, metadata, "admin"));
        verify(repo).updateMovie(1, metadata);
        for (var protectedEdit : List.of(
            edit("Other", "Hài", 90, "P", "DANG_CHIEU", null, null),
            edit("Phim", "Other", 90, "P", "DANG_CHIEU", null, null),
            edit("Phim", "Hài", 91, "P", "DANG_CHIEU", null, null),
            edit("Phim", "Hài", 90, "T18", "DANG_CHIEU", null, null),
            edit("Phim", "Hài", 90, "P", "NGUNG_CHIEU", null, null))) {
            assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.saveMovie(1L, protectedEdit, "admin")).getStatusCode().value());
        }
    }

    @Test
    void timelineUsesVietnamDayBoundariesAndCurrentAdminRole() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(new User(1, "admin", "", "QUAN_TRI", "HOAT_DONG"));
        service.timeline("admin", LocalDate.of(2026, 11, 9), 2L, "HUY");
        verify(repo).timeline(LocalDateTime.parse("2026-11-09T00:00:00"), LocalDateTime.parse("2026-11-10T00:00:00"), 2L, "HUY");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.timeline("admin", LocalDate.now(), 2L, "UNKNOWN")).getStatusCode().value());
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(new User(1, "admin", "", "NHAN_VIEN", "HOAT_DONG"));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.timeline("admin", LocalDate.now(), null, "")).getStatusCode().value());
    }
}
