package com.cinesnack.security;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.security.GuestOrderTokenService;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;

class GuestOrderTokenServiceTest {

    private final Clock clock = Clock.fixed(
        Instant.parse("2026-11-03T03:00:00Z"),
        ZoneOffset.UTC
    );
    private final GuestOrderTokenService tokens = new GuestOrderTokenService(
        "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        clock
    );

    @Test
    void paidGuestCanReadWithValidTokenButNotWithAnotherOrderOrExpiredToken() {
        Order paid = order("DH01", null, "DA_THANH_TOAN");
        String token = tokens.tao(paid);
        assertTrue(tokens.hopLe(paid, token));
        assertFalse(tokens.hopLe(order("DH02", null, "DA_THANH_TOAN"), token));
        Order otherEmail = new Order(
            1,
            null,
            "DH01",
            "other@example.com",
            BigDecimal.TEN,
            "DA_THANH_TOAN",
            LocalDateTime.now(clock),
            LocalDateTime.now(clock)
        );
        assertFalse(tokens.hopLe(otherEmail, token));
        assertFalse(tokens.hopLe(order("DH01", null, "CHO_THANH_TOAN"), token));
        assertFalse(tokens.hopLe(order("DH01", 9L, "DA_THANH_TOAN"), token));
        assertFalse(tokens.hopLe(paid, token + "x"));
        assertFalse(
            new GuestOrderTokenService(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                Clock.offset(clock, Duration.ofDays(91))
            ).hopLe(paid, token)
        );
    }

    private Order order(String code, Long userId, String status) {
        return new Order(
            1,
            userId,
            code,
            "guest@example.com",
            BigDecimal.TEN,
            status,
            LocalDateTime.now(clock),
            LocalDateTime.now(clock)
        );
    }
}
