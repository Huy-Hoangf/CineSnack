package com.cinesnack.service;

import com.cinesnack.entity.Invoice;
import com.cinesnack.entity.Order;
import com.cinesnack.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

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
        var hoaDon = khoHoaDon.timTheoDonHang(donHang.maSo());
        if (hoaDon.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn chưa có hóa đơn");
        return hoaDon;
    }
}
