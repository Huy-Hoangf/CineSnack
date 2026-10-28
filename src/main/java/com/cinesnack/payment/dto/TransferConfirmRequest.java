package com.cinesnack.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferConfirmRequest(
        @JsonProperty("transactionCode") @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{4,100}") String maGiaoDich,
        @JsonProperty("amountReceived") @NotNull @Positive BigDecimal soTienNhan) {
}
