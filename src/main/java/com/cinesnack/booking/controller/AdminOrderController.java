package com.cinesnack.booking.controller;

import com.cinesnack.booking.dto.CreateOrderRequest;
import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.repository.OrderRepository;
import com.cinesnack.booking.service.OrderExpiryService;
import com.cinesnack.booking.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService donHang;
    private final OrderExpiryService hetHan;

    public AdminOrderController(
        OrderService donHang,
        OrderExpiryService hetHan
    ) {
        this.donHang = donHang;
        this.hetHan = hetHan;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order taoDonTaiQuay(
        @Valid @RequestBody CreateOrderRequest yeuCau,
        Principal nguoiDung
    ) {
        hetHan.xuLyHetHan();
        return donHang.taoDonTaiQuay(yeuCau, nguoiDung.getName());
    }

    @GetMapping
    public List<Order> layDanhSach(
        @RequestParam(name = "q", defaultValue = "") @Size(
            max = 100
        ) String tuTim,
        @RequestParam(name = "status", defaultValue = "") @Pattern(
            regexp = "|CHO_THANH_TOAN|DA_THANH_TOAN|HUY"
        ) String trangThai,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(
            1000000
        ) int trang,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(
            100
        ) int soLuong,
        Principal nguoiDung
    ) {
        return donHang.layDanhSachQuanTri(
            nguoiDung.getName(),
            tuTim,
            trangThai,
            trang,
            soLuong
        );
    }

    @GetMapping("/snack-pickups")
    public List<OrderRepository.Pickup> choNhanBapNuoc(Principal user) {
        return donHang.choNhanBapNuoc(user.getName());
    }

    public record SnackScanRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,100}") String code
    ) {}

    public record SnackScanResult(int deliveredLines) {}

    @PostMapping("/snack-pickups/scan")
    public SnackScanResult scanSnacks(@Valid @RequestBody SnackScanRequest request, Principal user) {
        return new SnackScanResult(donHang.giaoTheoHoaDon(request.code(), user.getName()));
    }

    @PostMapping("/{code}/snacks/{itemId}/deliver")
    public OrderService.Details giaoBapNuoc(
        @PathVariable @Pattern(regexp = "[A-Za-z0-9]{1,30}") String code,
        @PathVariable @Min(1) long itemId,
        Principal user
    ) {
        return donHang.giaoBapNuoc(code, itemId, user.getName());
    }
}
