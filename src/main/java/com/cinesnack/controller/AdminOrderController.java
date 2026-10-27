package com.cinesnack.controller;

import com.cinesnack.entity.Order;
import com.cinesnack.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import com.cinesnack.service.OrderService;
import com.cinesnack.service.OrderExpiryService;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class AdminOrderController {

    private final OrderService donHang;
    private final OrderExpiryService hetHan;

    public AdminOrderController(OrderService donHang, OrderExpiryService hetHan) {
        this.donHang = donHang;
        this.hetHan = hetHan;
    }

    @PostMapping("/api/admin/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order taoDonTaiQuay(@Valid @RequestBody CreateOrderRequest yeuCau, Principal nguoiDung) {
        hetHan.xuLyHetHan();
        return donHang.taoDonTaiQuay(yeuCau, nguoiDung.getName());
    }

    @GetMapping("/api/admin/orders")
    public List<Order> layDanhSach(
        @RequestParam(name = "q", defaultValue = "") @Size(max = 100) String tuTim,
        @RequestParam(name = "status", defaultValue = "") @Pattern(
            regexp = "|CHO_THANH_TOAN|DA_THANH_TOAN|HUY"
        ) String trangThai,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000000) int trang,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int soLuong,
        Principal nguoiDung
    ) {
        return donHang.layDanhSachQuanTri(nguoiDung.getName(), tuTim, trangThai, trang, soLuong);
    }
}
