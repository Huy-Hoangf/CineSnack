package com.cinesnack.invoice.service;

import com.cinesnack.invoice.entity.Invoice;
import com.cinesnack.booking.entity.Order;
import com.cinesnack.invoice.repository.InvoiceRepository;
import com.cinesnack.booking.service.OrderService;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoiceService {

    private final InvoiceRepository khoHoaDon;
    private final OrderService dichVuDonHang;

    public InvoiceService(InvoiceRepository khoHoaDon, OrderService dichVuDonHang) {
        this.khoHoaDon = khoHoaDon;
        this.dichVuDonHang = dichVuDonHang;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void phatHanh(Order donHang, LocalDateTime hienTai) {
        khoHoaDon.taoHoaDon(donHang, hienTai);
    }

    @Transactional(readOnly = true)
    public java.util.List<Invoice> layHoaDon(String ma, String thuDienTu) {
        Order donHang = dichVuDonHang.layDonDuocPhepTruyCap(ma, thuDienTu);
        return layHoaDon(donHang);
    }

    @Transactional(readOnly = true)
    public java.util.List<Invoice> layHoaDonKhach(String ma, Set<String> maDonTrongPhien) {
        return layHoaDon(dichVuDonHang.layDonKhachDuocPhepTruyCap(ma, maDonTrongPhien));
    }

    private java.util.List<Invoice> layHoaDon(Order donHang) {
        var hoaDon = khoHoaDon.timTheoDonHang(donHang.maSo());
        if (hoaDon.isEmpty()) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Đơn chưa có hóa đơn"
        );
        return hoaDon;
    }
}
