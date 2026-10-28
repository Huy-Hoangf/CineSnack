package com.cinesnack.payment.controller;

import com.cinesnack.payment.dto.CashPaymentRequest;
import com.cinesnack.payment.dto.TransferConfirmRequest;
import com.cinesnack.payment.entity.Payment;
import com.cinesnack.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{code}/payments")
@Validated
public class PaymentController {

    private final PaymentService dichVuThanhToan;

    public PaymentController(PaymentService dichVuThanhToan) {
        this.dichVuThanhToan = dichVuThanhToan;
    }

    @PostMapping("/cash")
    public PaymentService.Receipt thanhToanTaiQuay(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        @Valid @RequestBody CashPaymentRequest yeuCau,
        Principal nguoiDung
    ) {
        return dichVuThanhToan.thanhToanTaiQuay(
            maDonHang,
            yeuCau.tienKhachDua(),
            nguoiDung.getName()
        );
    }

    @PostMapping("/transfer")
    public Payment xacNhanChuyenKhoan(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        @Valid @RequestBody TransferConfirmRequest yeuCau,
        Principal nguoiDung
    ) {
        return dichVuThanhToan.xacNhanChuyenKhoan(maDonHang, yeuCau.maGiaoDich(), yeuCau.soTienNhan(), nguoiDung.getName());
    }

    @GetMapping
    public List<Payment> layDanhSach(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        Principal nguoiDung
    ) {
        return dichVuThanhToan.layDanhSach(maDonHang, nguoiDung.getName());
    }
}
