package com.cinesnack.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class GmailEmailClientTest {

    private final PaymentConfirmed payment = new PaymentConfirmed(
        "DH01",
        "customer@example.com",
        new BigDecimal("229000")
    );

    @Test
    @SuppressWarnings("unchecked")
    void refreshesTokenThenSendsEncodedOrderEmail() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        HttpResponse<String> sendResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn(
            "{\"access_token\":\"access-token\"}"
        );
        when(sendResponse.statusCode()).thenReturn(200);
        when(
            http.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
            )
        ).thenReturn(tokenResponse, sendResponse);

        var client = new GmailEmailClient(
            "client-id",
            "client-secret",
            "refresh-token",
            "sender@gmail.com",
            new ObjectMapper(),
            http
        );
        client.send(payment);

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(
            HttpRequest.class
        );
        verify(http, times(2)).send(
            requests.capture(),
            any(HttpResponse.BodyHandler.class)
        );
        assertEquals(
            "https://oauth2.googleapis.com/token",
            requests.getAllValues().get(0).uri().toString()
        );
        assertEquals(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
            requests.getAllValues().get(1).uri().toString()
        );
        assertEquals(
            "Bearer access-token",
            requests
                .getAllValues()
                .get(1)
                .headers()
                .firstValue("Authorization")
                .orElseThrow()
        );

        String json = client.messageJson(payment);
        String raw = new ObjectMapper().readTree(json).get("raw").asString();
        String mime = new String(
            Base64.getUrlDecoder().decode(raw),
            StandardCharsets.UTF_8
        );
        assertTrue(mime.contains("To: customer@example.com"));
        assertTrue(mime.contains("From: CineSnack <sender@gmail.com>"));
        assertTrue(
            mime.contains(
                Base64.getEncoder().encodeToString(
                    "Xác nhận thanh toán đơn DH01".getBytes(
                        StandardCharsets.UTF_8
                    )
                )
            )
        );
        String encodedBody = mime.substring(mime.indexOf("\r\n\r\n") + 4);
        String body = new String(
            Base64.getMimeDecoder().decode(encodedBody),
            StandardCharsets.UTF_8
        );
        assertEquals(
            "CineSnack xác nhận đơn DH01 đã thanh toán thành công. Tổng tiền: 229000 VND.",
            body
        );
    }

    @Test
    void missingCredentialsDoesNotCallApi() {
        HttpClient http = mock(HttpClient.class);
        new GmailEmailClient("", "", "", "", new ObjectMapper(), http).send(
            payment
        );
        verifyNoInteractions(http);
    }

    @Test
    void guestEmailIncludesRecoveryLink() {
        var client = new GmailEmailClient(
            "id",
            "secret",
            "refresh",
            "sender@gmail.com",
            "https://cinema.example",
            new ObjectMapper(),
            mock(HttpClient.class)
        );
        String json = client.messageJson(
            new PaymentConfirmed(
                "DH01",
                "guest@example.com",
                BigDecimal.TEN,
                "123.token"
            )
        );
        String raw = new ObjectMapper().readTree(json).get("raw").asString();
        String mime = new String(
            Base64.getUrlDecoder().decode(raw),
            StandardCharsets.UTF_8
        );
        String body = new String(
            Base64.getMimeDecoder().decode(
                mime.substring(mime.indexOf("\r\n\r\n") + 4)
            ),
            StandardCharsets.UTF_8
        );
        assertTrue(
            body.contains(
                "https://cinema.example/html/orders.html?code=DH01&guest=1#access=123.token"
            )
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void tokenFailureDoesNotAttemptToSendEmail() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(400);
        when(
            http.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
            )
        ).thenReturn(tokenResponse);

        new GmailEmailClient(
            "client-id",
            "client-secret",
            "refresh-token",
            "sender@gmail.com",
            new ObjectMapper(),
            http
        ).send(payment);

        verify(http, times(1)).send(
            any(HttpRequest.class),
            any(HttpResponse.BodyHandler.class)
        );
    }
}
