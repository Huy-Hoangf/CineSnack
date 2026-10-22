package com.cinesnack.controller;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.entity.Invoice;
import com.cinesnack.entity.Order;
import com.cinesnack.security.GuestOrderAccess;
import com.cinesnack.service.InvoiceService;
import com.cinesnack.service.OrderExpiryService;
import com.cinesnack.service.OrderService;
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

    public GuestOrderController(
        OrderService donHang,
        InvoiceService hoaDon,
        OrderExpiryService hetHan,
        GuestOrderAccess quyenKhach
    ) {
        this.donHang = donHang;
        this.hoaDon = hoaDon;
        this.hetHan = hetHan;
        this.quyenKhach = quyenKhach;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order taoDonHang(@Valid @RequestBody CreateOrderRequest yeuCau, HttpSession session) {
        hetHan.xuLyHetHan();
        Order don = donHang.taoDonKhach(yeuCau);
        quyenKhach.ghiNhanDonDaTao(session, don);
        return don;
    }

    @GetMapping
    public List<Order> layDanhSach(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000000) int trang,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int kichThuoc,
        HttpSession session
    ) {
        return donHang.layDanhSachKhach(quyenKhach.layMaDon(session), trang, kichThuoc);
    }

    @GetMapping("/{code}")
    public OrderService.Details layChiTiet(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String ma,
        HttpSession session
    ) {
        return donHang.layChiTietKhach(ma, quyenKhach.layMaDon(session));
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
        HttpSession session
    ) {
        return hoaDon.layHoaDonKhach(ma, quyenKhach.layMaDon(session));
    }
}
