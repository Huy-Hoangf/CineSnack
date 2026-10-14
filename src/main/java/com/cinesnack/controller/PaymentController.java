package com.cinesnack.controller;

import com.cinesnack.dto.CashPaymentRequest;
import com.cinesnack.entity.Payment;
import com.cinesnack.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders/{code}/payments")
@Validated
public class PaymentController {
    private final PaymentService dichVuThanhToan;

    public PaymentController(PaymentService dichVuThanhToan) { this.dichVuThanhToan = dichVuThanhToan; }

    @PostMapping("/cash")
    public PaymentService.Receipt thanhToanTaiQuay(@PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
                                      @Valid @RequestBody CashPaymentRequest yeuCau, Principal nguoiDung) {
        return dichVuThanhToan.thanhToanTaiQuay(maDonHang, yeuCau.tienKhachDua(), nguoiDung.getName());
    }

    @GetMapping
    public List<Payment> layDanhSach(@PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang, Principal nguoiDung) {
        return dichVuThanhToan.layDanhSach(maDonHang, nguoiDung.getName());
    }
}
