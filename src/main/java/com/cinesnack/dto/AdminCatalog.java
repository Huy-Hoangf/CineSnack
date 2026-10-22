package com.cinesnack.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class AdminCatalog {

    private AdminCatalog() {}

    public record MovieRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String genre,
        @Min(1) @Max(600) int duration,
        @NotBlank @Pattern(regexp = "P|K|T13|T16|T18|C") String ageRating,
        @Size(max = 500)
        @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
        String posterUrl,
        @NotNull @Pattern(regexp = "DANG_CHIEU|SAP_CHIEU|NGUNG_CHIEU") String status
    ) {}

    public record ShowRequest(
        @NotNull @Positive Long movieId,
        @NotNull @Positive Long roomId,
        @NotNull OffsetDateTime startsAt,
        @NotNull
        @DecimalMin("0")
        @DecimalMax("9999999999")
        @Digits(integer = 10, fraction = 0)
        BigDecimal price,
        @NotNull @Pattern(regexp = "SAP_CHIEU|DANG_CHIEU|DA_CHIEU|HUY") String status
    ) {}

    public record Room(long id, String name) {}
}
