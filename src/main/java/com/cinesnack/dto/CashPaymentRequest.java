package com.cinesnack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CashPaymentRequest(
    @JsonProperty("amountReceived")
    @NotNull
    @DecimalMin("0")
    @Digits(integer = 12, fraction = 0)
    BigDecimal tienKhachDua
) {}
