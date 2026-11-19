package com.cinesnack.booking.controller;

import com.cinesnack.booking.dto.CreateOrderRequest;
import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.security.GuestOrderAccess;
import com.cinesnack.booking.security.GuestOrderTokenService;
import com.cinesnack.booking.service.OrderExpiryService;
import com.cinesnack.booking.service.OrderService;
import com.cinesnack.invoice.entity.Invoice;
import com.cinesnack.invoice.service.InvoiceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guest/orders")
@Validated
public class GuestOrderController {

    private final OrderService donHang;
    private final InvoiceService hoaDon;
    private final OrderExpiryService hetHan;
    private final GuestOrderAccess quyenKhach;
    private final GuestOrderTokenService guestTokens;

    public GuestOrderController(
        OrderService donHang,
        InvoiceService hoaDon,
        OrderExpiryService hetHan,
        GuestOrderAccess quyenKhach,
        GuestOrderTokenService guestTokens
    ) {
        this.donHang = donHang;
        this.hoaDon = hoaDon;
        this.hetHan = hetHan;
        this.quyenKhach = quyenKhach;
        this.guestTokens = guestTokens;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order taoDonHang(
        @Valid @RequestBody CreateOrderRequest yeuCau,
        HttpSession session
    ) {
        hetHan.xuLyHetHan();
        Order don = donHang.taoDonKhach(yeuCau);
        quyenKhach.ghiNhanDonDaTao(session, don);
        return don;
    }

    @GetMapping
    public List<Order> layDanhSach(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(
            1000000
        ) int trang,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(
            100
        ) int kichThuoc,
        HttpSession session
    ) {
        return donHang.layDanhSachKhach(
            quyenKhach.layMaDon(session),
            trang,
            kichThuoc
        );
    }

    @GetMapping("/{code}")
    public OrderService.Details layChiTiet(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        @RequestHeader(
            name = "X-Guest-Access-Token",
            required = false
        ) String token,
        HttpSession session
    ) {
        return donHang.layChiTietKhach(ma, duocPhepDoc(ma, token, session));
    }

    @PostMapping("/{code}/cancel")
    public Order huyDonHang(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        HttpSession session
    ) {
        return donHang.huyDonKhach(ma, quyenKhach.layMaDon(session));
    }

    @GetMapping("/{code}/invoices")
    public List<Invoice> layHoaDon(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        @RequestHeader(
            name = "X-Guest-Access-Token",
            required = false
        ) String token,
        HttpSession session
    ) {
        return hoaDon.layHoaDonKhach(ma, duocPhepDoc(ma, token, session));
    }

    private java.util.Set<String> duocPhepDoc(
        String ma,
        String token,
        HttpSession session
    ) {
        var trongPhien = quyenKhach.layMaDon(session);
        return trongPhien.contains(ma) || guestTokens.hopLe(ma, token)
            ? java.util.Set.of(ma)
            : java.util.Set.of();
    }
}
