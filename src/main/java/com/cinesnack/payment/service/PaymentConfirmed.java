package com.cinesnack.payment.service;

import java.math.BigDecimal;

public record PaymentConfirmed(
    String orderCode,
    String recipient,
    BigDecimal total,
    String guestAccessToken
) {
    public PaymentConfirmed(
        String orderCode,
        String recipient,
        BigDecimal total
    ) {
        this(orderCode, recipient, total, null);
    }
}
