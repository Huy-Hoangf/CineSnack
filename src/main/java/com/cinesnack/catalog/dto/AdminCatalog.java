package com.cinesnack.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;

public final class AdminCatalog {

    private AdminCatalog() {}

    public record MovieRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String genre,
        @Size(max = 2000) String description,
        @Size(max = 500) String actors,
        @Min(1) @Max(600) int duration,
        @NotBlank @Pattern(regexp = "P|K|T13|T16|T18|C") String ageRating,
        @Size(max = 500)
        @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
        String posterUrl,
        @NotNull
        @Pattern(regexp = "DANG_CHIEU|SAP_CHIEU|NGUNG_CHIEU")
        String status,
        @Size(max = 200) String director,
        @Size(max = 500) String trailerUrl
    ) {
        public MovieRequest(String name, String genre, String description, String actors,
            int duration, String ageRating, String posterUrl, String status) {
            this(name, genre, description, actors, duration, ageRating, posterUrl, status, null, null);
        }

        @AssertTrue(message = "Trailer phải là URL HTTPS có máy chủ và không chứa tài khoản.")
        @JsonIgnore
        public boolean isTrailerUrlValid() {
            if (trailerUrl == null || trailerUrl.isEmpty()) return true;
            try {
                URI uri = URI.create(trailerUrl);
                return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }

    public record ShowRequest(
        @NotNull @Positive Long movieId,
        @NotNull @Positive Long roomId,
        @NotNull OffsetDateTime startsAt,
        @DecimalMin("0")
        @DecimalMax("9999999999")
        @Digits(integer = 10, fraction = 0)
        BigDecimal price,
        @DecimalMin("0")
        @DecimalMax("9999999999")
        @Digits(integer = 10, fraction = 0)
        BigDecimal vipSurcharge,
        @NotNull
        @Pattern(regexp = "SAP_CHIEU|DANG_CHIEU|DA_CHIEU|HUY")
        String status,
        @Pattern(regexp = "MANUAL|POLICY") String pricingMode
    ) {
        public ShowRequest { pricingMode = pricingMode == null ? "MANUAL" : pricingMode; }
        public ShowRequest(Long movieId, Long roomId, OffsetDateTime startsAt, BigDecimal price, BigDecimal vipSurcharge, String status) {
            this(movieId, roomId, startsAt, price, vipSurcharge, status, "MANUAL");
        }
        @AssertTrue(message = "Chế độ nhập tay cần giá cơ bản.")
        @JsonIgnore
        public boolean isManualPriceValid() { return !"MANUAL".equals(pricingMode) || price != null; }
        public ShowRequest(
            Long movieId,
            Long roomId,
            OffsetDateTime startsAt,
            BigDecimal price,
            String status
        ) {
            this(movieId, roomId, startsAt, price, BigDecimal.ZERO, status);
        }

        public BigDecimal vipSurcharge() {
            return vipSurcharge == null ? BigDecimal.ZERO : vipSurcharge;
        }
    }

    public record Room(long id, String name) {}
}
