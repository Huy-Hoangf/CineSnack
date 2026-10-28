package com.cinesnack.booking.security;

import com.cinesnack.booking.entity.Order;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GuestOrderAccess {

    private static final String SESSION_KEY = "cinesnack.guest.orders";
    private static final int MAX_GUEST_ORDERS_PER_SESSION = 100;

    public void ghiNhanDonDaTao(HttpSession session, Order donHang) {
        if (donHang.maNguoiDung() != null) {
            throw new IllegalArgumentException("Chỉ ghi nhận đơn khách vãng lai");
        }
        synchronized (session) {
            @SuppressWarnings("unchecked")
            Set<String> maDon = (Set<String>) session.getAttribute(SESSION_KEY);
            if (maDon == null) {
                maDon = new LinkedHashSet<>();
                session.setAttribute(SESSION_KEY, maDon);
            }
            maDon.add(donHang.ma());
            while (maDon.size() > MAX_GUEST_ORDERS_PER_SESSION) {
                maDon.remove(maDon.iterator().next());
            }
        }
    }

    public Set<String> layMaDon(HttpSession session) {
        if (session == null) return Set.of();
        synchronized (session) {
            @SuppressWarnings("unchecked")
            Set<String> maDon = (Set<String>) session.getAttribute(SESSION_KEY);
            return maDon == null ? Set.of() : Set.copyOf(maDon);
        }
    }
}
