package com.cinesnack.payment.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class GmailEmailClient {

    private static final Logger log = LoggerFactory.getLogger(
        GmailEmailClient.class
    );
    private static final URI TOKEN_API = URI.create(
        "https://oauth2.googleapis.com/token"
    );
    private static final URI SEND_API = URI.create(
        "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    );

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String senderEmail;
    private final String publicBaseUrl;
    private final ObjectMapper mapper;
    private final HttpClient http;

    @Autowired
    public GmailEmailClient(
        @Value("${mail.gmail.client-id:}") String clientId,
        @Value("${mail.gmail.client-secret:}") String clientSecret,
        @Value("${mail.gmail.refresh-token:}") String refreshToken,
        @Value("${mail.gmail.sender-email:}") String senderEmail,
        @Value(
            "${app.public-base-url:http://localhost:8080}"
        ) String publicBaseUrl,
        ObjectMapper mapper
    ) {
        this(
            clientId,
            clientSecret,
            refreshToken,
            senderEmail,
            publicBaseUrl,
            mapper,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
        );
    }

    GmailEmailClient(
        String clientId,
        String clientSecret,
        String refreshToken,
        String senderEmail,
        ObjectMapper mapper,
        HttpClient http
    ) {
        this(
            clientId,
            clientSecret,
            refreshToken,
            senderEmail,
            "http://localhost:8080",
            mapper,
            http
        );
    }

    GmailEmailClient(
        String clientId,
        String clientSecret,
        String refreshToken,
        String senderEmail,
        String publicBaseUrl,
        ObjectMapper mapper,
        HttpClient http
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.senderEmail = senderEmail;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.mapper = mapper;
        this.http = http;
    }

    public void send(PaymentConfirmed payment) {
        if (
            clientId.isBlank() ||
            clientSecret.isBlank() ||
            refreshToken.isBlank() ||
            senderEmail.isBlank()
        ) {
            log.warn(
                "Gmail chưa được cấu hình; không thể gửi email cho đơn {}",
                payment.orderCode()
            );
            return;
        }
        try {
            String accessToken = accessToken();
            HttpRequest request = HttpRequest.newBuilder(SEND_API)
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(messageJson(payment)))
                .build();
            HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                log.error(
                    "Gmail từ chối email cho đơn {} (HTTP {})",
                    payment.orderCode(),
                    response.statusCode()
                );
            } else {
                log.info(
                    "Gmail đã nhận email xác nhận đơn {}",
                    payment.orderCode()
                );
            }
        } catch (JacksonException | IOException | IllegalArgumentException e) {
            log.error(
                "Không gửi được email cho đơn {}: {}",
                payment.orderCode(),
                e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(
                "Gửi email cho đơn {} bị gián đoạn",
                payment.orderCode(),
                e
            );
        }
    }

    private String accessToken() throws IOException, InterruptedException {
        String form =
            "client_id=" +
            encode(clientId) +
            "&client_secret=" +
            encode(clientSecret) +
            "&refresh_token=" +
            encode(refreshToken) +
            "&grant_type=refresh_token";
        HttpRequest request = HttpRequest.newBuilder(TOKEN_API)
            .timeout(Duration.ofSeconds(8))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> response = http.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            throw new IOException(
                "Google OAuth trả về HTTP " + response.statusCode()
            );
        }
        String token = mapper
            .readTree(response.body())
            .path("access_token")
            .asString();
        if (token.isBlank()) {
            throw new IOException("Google OAuth không trả về access token");
        }
        return token;
    }

    String messageJson(PaymentConfirmed payment) {
        String recipient = safeAddress(payment.recipient());
        String sender = safeAddress(senderEmail);
        String subject = "Xác nhận thanh toán đơn " + payment.orderCode();
        String content =
            "CineSnack xác nhận đơn " +
            payment.orderCode() +
            " đã thanh toán thành công. Tổng tiền: " +
            payment.total().toPlainString() +
            " VND.";
        if (payment.guestAccessToken() != null) {
            content +=
                "\nXem lại vé và hóa đơn: " +
                publicBaseUrl +
                "/html/orders.html?code=" +
                encode(payment.orderCode()) +
                "&guest=1#access=" +
                payment.guestAccessToken() +
                "\nKhông chia sẻ liên kết này; liên kết có hiệu lực 90 ngày.";
        }
        String mime =
            "From: CineSnack <" +
            sender +
            ">\r\n" +
            "To: " +
            recipient +
            "\r\n" +
            "Subject: =?UTF-8?B?" +
            Base64.getEncoder().encodeToString(
                subject.getBytes(StandardCharsets.UTF_8)
            ) +
            "?=\r\n" +
            "MIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "Content-Transfer-Encoding: base64\r\n\r\n" +
            Base64.getMimeEncoder(
                76,
                "\r\n".getBytes(StandardCharsets.US_ASCII)
            ).encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String raw = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mime.getBytes(StandardCharsets.UTF_8));
        return mapper.writeValueAsString(Map.of("raw", raw));
    }

    private static String safeAddress(String value) {
        if (
            value == null ||
            !value.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")
        ) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ");
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
