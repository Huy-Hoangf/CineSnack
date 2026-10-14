package com.cinesnack.controller;
import com.cinesnack.entity.Order;
import com.cinesnack.service.OrderService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
@RestController @Validated
public class AdminOrderController {
    private final OrderService donHang;
    public AdminOrderController(OrderService donHang) { this.donHang=donHang; }
    @GetMapping("/api/admin/orders")
    public List<Order> layDanhSach(@RequestParam(name="page",defaultValue="0") @Min(0) @Max(1000000) int trang,
        @RequestParam(name="size",defaultValue="20") @Min(1) @Max(100) int soLuong, Principal nguoiDung) {
        return donHang.layDanhSachQuanTri(nguoiDung.getName(),trang,soLuong);
    }
}
