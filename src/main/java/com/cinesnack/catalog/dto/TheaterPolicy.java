package com.cinesnack.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class TheaterPolicy {
    private TheaterPolicy() {}
    public record RoomPricing(
        @NotNull @DecimalMin("0") @DecimalMax("9999999999") @Digits(integer=10, fraction=0) BigDecimal weekdayRegular,
        @NotNull @DecimalMin("0") @DecimalMax("9999999999") @Digits(integer=10, fraction=0) BigDecimal weekdayVip,
        @NotNull @DecimalMin("0") @DecimalMax("9999999999") @Digits(integer=10, fraction=0) BigDecimal weekendRegular,
        @NotNull @DecimalMin("0") @DecimalMax("9999999999") @Digits(integer=10, fraction=0) BigDecimal weekendVip
    ) {
        @JsonIgnore
        @AssertTrue(message="Giá VIP phải lớn hơn hoặc bằng giá thường cùng loại ngày.")
        public boolean isVipPriceValid() {
            return (weekdayRegular == null || weekdayVip == null || weekdayVip.compareTo(weekdayRegular) >= 0)
                && (weekendRegular == null || weekendVip == null || weekendVip.compareTo(weekendRegular) >= 0);
        }
    }
    public record Settings(@Min(0) @Max(600) Integer lateCheckInMinutes) {
        // Integer's default JSON coercion truncates fractions; minutes must be exact.
        @JsonCreator
        public static Settings fromJson(@JsonProperty("lateCheckInMinutes") BigDecimal minutes) {
            return new Settings(minutes == null ? null : minutes.intValueExact());
        }
    }
    public record ShowPrices(BigDecimal regular, BigDecimal vip) {}
}
