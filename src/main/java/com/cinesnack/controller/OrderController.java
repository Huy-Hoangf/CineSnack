package com.cinesnack.controller;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.entity.Order;
import com.cinesnack.service.OrderExpiryService;
import com.cinesnack.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService dichVuDonHang;
    private final OrderExpiryService hetHan;

    public OrderController(OrderService dichVuDonHang, OrderExpiryService hetHan) {
        this.dichVuDonHang = dichVuDonHang;
        this.hetHan = hetHan;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order taoDonHang(@Valid @RequestBody CreateOrderRequest yeuCau, Principal nguoiDung) {
        hetHan.xuLyHetHan();
        return dichVuDonHang.taoDonHang(yeuCau, nguoiDung.getName());
    }

    @GetMapping
    public List<Order> layDanhSach(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000000) int trang,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int kichThuoc,
        Principal nguoiDung
    ) {
        return dichVuDonHang.layDanhSach(nguoiDung.getName(), trang, kichThuoc);
    }

    @GetMapping("/{code}")
    public OrderService.Details layChiTiet(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        Principal nguoiDung
    ) {
        return dichVuDonHang.layChiTiet(maDonHang, nguoiDung.getName());
    }

    @PostMapping("/{code}/cancel")
    public Order huyDonHang(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        Principal nguoiDung
    ) {
        return dichVuDonHang.huyDonHang(maDonHang, nguoiDung.getName());
    }
}
