package com.cinesnack.payment.controller;

import com.cinesnack.booking.security.GuestOrderAccess;
import com.cinesnack.payment.service.QrPaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class QrPaymentController {

    private final QrPaymentService qr;
    private final GuestOrderAccess quyenKhach;

    public QrPaymentController(QrPaymentService qr, GuestOrderAccess quyenKhach) {
        this.qr = qr;
        this.quyenKhach = quyenKhach;
    }

    @GetMapping("/orders/{code}/payment-qr")
    public QrPaymentService.QrPayment layQr(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        Principal nguoiDung
    ) {
        return qr.layQr(ma, nguoiDung.getName());
    }

    @GetMapping("/guest/orders/{code}/payment-qr")
    public QrPaymentService.QrPayment layQrKhach(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        HttpSession session
    ) {
        return qr.layQrKhach(ma, quyenKhach.layMaDon(session));
    }
}
