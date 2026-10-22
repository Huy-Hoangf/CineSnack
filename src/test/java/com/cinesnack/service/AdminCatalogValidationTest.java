package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.dto.AdminCatalog;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class AdminCatalogValidationTest {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void movieRejectsUnsafePosterAndUnknownStatus() {
        var request = new AdminCatalog.MovieRequest(
            "Phim",
            "Hài",
            100,
            "T13",
            "javascript:alert(1)",
            "UNKNOWN"
        );
        var fields = validator
            .validate(request)
            .stream()
            .map(v -> v.getPropertyPath().toString())
            .toList();
        assertTrue(fields.contains("posterUrl"));
        assertTrue(fields.contains("status"));
    }

    @Test
    void scheduleRejectsFractionalCashPriceAndInvalidIdentifiers() {
        var request = new AdminCatalog.ShowRequest(
            0L,
            -1L,
            OffsetDateTime.parse("2026-10-15T10:00:00+07:00"),
            new BigDecimal("0.5"),
            "SAP_CHIEU"
        );
        var fields = validator
            .validate(request)
            .stream()
            .map(v -> v.getPropertyPath().toString())
            .toList();
        assertTrue(fields.contains("movieId"));
        assertTrue(fields.contains("roomId"));
        assertTrue(fields.contains("price"));
    }

    @Test
    void validMovieAndScheduleMatchDatabaseStatuses() {
        assertTrue(
            validator
                .validate(
                    new AdminCatalog.MovieRequest(
                        "Phim",
                        "Hài",
                        100,
                        "P",
                        "/assets/images/movies/dune.jpg",
                        "NGUNG_CHIEU"
                    )
                )
                .isEmpty()
        );
        assertTrue(
            validator
                .validate(
                    new AdminCatalog.ShowRequest(
                        1L,
                        2L,
                        OffsetDateTime.parse("2026-10-15T10:00:00+07:00"),
                        new BigDecimal("80000"),
                        "DA_CHIEU"
                    )
                )
                .isEmpty()
        );
    }
}
