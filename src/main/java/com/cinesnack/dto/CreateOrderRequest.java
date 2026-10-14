package com.cinesnack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @JsonProperty("email") @NotBlank @Email @Size(max = 150) String thuDienTu,
        @JsonProperty("showTimeId") @Positive Long maSuatChieu,
        @JsonProperty("seatIds") @NotNull @Size(max = 10) List<@NotNull @Positive Long> dsMaGhe,
        @JsonProperty("snacks") @NotNull @Size(max = 20) List<@NotNull @Valid SnackChoice> dsBapNuoc,
        @JsonProperty("combos") @NotNull @Size(max = 20) List<@NotNull @Valid ComboChoice> dsCombo) {

    public record SnackChoice(
        @JsonProperty("productId") @NotNull @Positive Long maSanPham,
        @JsonProperty("quantity") @Min(1) @Max(10) int soLuong) {}

    public record ComboChoice(
        @JsonProperty("comboId") @NotNull @Positive Long maCombo,
        @JsonProperty("quantity") @Min(1) @Max(10) int soLuong) {}
}
