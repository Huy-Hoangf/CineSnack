package com.cinesnack.booking.security;

import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.repository.OrderRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GuestOrderTokenService {

    private static final Duration VALIDITY = Duration.ofDays(90);
    private final byte[] secret;
    private final Clock clock;
    private final OrderRepository orders;

    @Autowired
    public GuestOrderTokenService(
        @Value("${security.guest-link-secret:}") String encodedSecret,
        Clock clock,
        OrderRepository orders
    ) {
        this.secret = encodedSecret.isBlank()
            ? new byte[0]
            : Base64.getDecoder().decode(encodedSecret);
        if (secret.length != 0 && secret.length < 32) {
            throw new IllegalArgumentException(
                "Khóa liên kết đơn guest phải có ít nhất 32 byte"
            );
        }
        this.clock = clock;
        this.orders = orders;
    }

    public GuestOrderTokenService(String encodedSecret, Clock clock) {
        this(encodedSecret, clock, null);
    }

    public boolean hopLe(String code, String token) {
        return orders != null && hopLe(orders.timTheoMa(code), token);
    }

    public String tao(Order order) {
        if (
            secret.length == 0 ||
            order.maNguoiDung() != null ||
            order.thuDienTu() == null
        ) return null;
        long expires = Instant.now(clock).plus(VALIDITY).getEpochSecond();
        return (
            expires +
            "." +
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(sign(order, expires))
        );
    }

    public boolean hopLe(Order order, String token) {
        if (
            secret.length == 0 ||
            order == null ||
            order.maNguoiDung() != null ||
            !"DA_THANH_TOAN".equals(order.trangThai()) ||
            order.thuDienTu() == null ||
            token == null ||
            !token.matches("[0-9]{1,12}\\.[A-Za-z0-9_-]{43}")
        ) return false;
        int dot = token.indexOf('.');
        try {
            long expires = Long.parseLong(token.substring(0, dot));
            if (
                expires <= Instant.now(clock).getEpochSecond() ||
                expires > Instant.now(clock).plus(VALIDITY).getEpochSecond()
            ) return false;
            byte[] actual = Base64.getUrlDecoder().decode(
                token.substring(dot + 1)
            );
            return MessageDigest.isEqual(sign(order, expires), actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] sign(Order order, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String data =
                order.ma() + "\n" + order.thuDienTu() + "\n" + expires;
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(
                "Không thể ký liên kết đơn guest",
                e
            );
        }
    }
}
