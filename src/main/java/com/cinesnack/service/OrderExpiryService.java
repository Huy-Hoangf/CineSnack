package com.cinesnack.service;

import com.cinesnack.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OrderExpiryService {
    private final OrderRepository khoDonHang;
    private final TransactionTemplate giaoDich;
    private final Clock dongHo;

    public OrderExpiryService(OrderRepository khoDonHang, TransactionTemplate giaoDich, Clock dongHo) {
        this.khoDonHang = khoDonHang;
        this.giaoDich = giaoDich;
        this.dongHo = dongHo;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void xuLyHetHan() {
        LocalDateTime hienTai = LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
        for (String ma : khoDonHang.layMaDonHetHan(hienTai)) {
            // Mỗi đơn có transaction riêng; cùng khóa với thanh toán và hủy đơn.
            giaoDich.executeWithoutResult(trangThai -> {
                var donHang = khoDonHang.timVaKhoa(ma);
                if (donHang != null && "CHO_THANH_TOAN".equals(donHang.trangThai())
                        && donHang.hetHanLuc() != null && !donHang.hetHanLuc().isAfter(hienTai)
                        && khoDonHang.doiTrangThai(donHang.maSo(), "CHO_THANH_TOAN", "HET_HAN") == 1) {
                    khoDonHang.dongChiTiet(donHang.maSo(), "HET_HAN");
                }
            });
        }
    }
}
