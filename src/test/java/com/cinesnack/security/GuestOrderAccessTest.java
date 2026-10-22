package com.cinesnack.security;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class GuestOrderAccessTest {

    @Test
    void keepsOnlyTheMostRecentHundredGuestOrdersInSession() {
        var access = new GuestOrderAccess();
        var session = new MockHttpSession();

        for (int i = 0; i < 101; i++) {
            access.ghiNhanDonDaTao(session, order("GUEST" + i));
        }

        var codes = access.layMaDon(session);
        assertEquals(100, codes.size());
        assertFalse(codes.contains("GUEST0"));
        assertTrue(codes.contains("GUEST100"));
    }

    private Order order(String code) {
        var now = LocalDateTime.parse("2026-10-20T10:00:00");
        return new Order(1, null, code, "guest@example.com", BigDecimal.TEN, "CHO_THANH_TOAN", now, now);
    }
}
