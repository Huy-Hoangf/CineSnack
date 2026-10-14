package com.cinesnack.controller;

import com.cinesnack.entity.Invoice;
import com.cinesnack.service.InvoiceService;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class InvoiceController {

    private final InvoiceService dichVuHoaDon;

    public InvoiceController(InvoiceService dichVuHoaDon) {
        this.dichVuHoaDon = dichVuHoaDon;
    }

    @GetMapping("/api/orders/{code}/invoices")
    public java.util.List<Invoice> layHoaDon(
        @PathVariable("code") @Pattern(regexp = "[A-Za-z0-9]{1,30}") String maDonHang,
        Principal nguoiDung
    ) {
        return dichVuHoaDon.layHoaDon(maDonHang, nguoiDung.getName());
    }
}
