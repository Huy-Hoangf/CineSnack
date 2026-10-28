package com.cinesnack.payment.service;

import com.cinesnack.booking.service.OrderService;

import com.cinesnack.booking.entity.Order;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QrPaymentService {

    public record QrPayment(
        @JsonProperty("amount") BigDecimal soTien,
        @JsonProperty("content") String noiDung,
        @JsonProperty("imageUrl") String diaChiAnh
    ) {}

    private final OrderService donHang;
    private final String maNganHang;
    private final String soTaiKhoan;
    private final String tenChuTaiKhoan;
    private final Clock dongHo;

    public QrPaymentService(OrderService donHang,
        @Value("${payment.qr.bank-id:}") String maNganHang,
        @Value("${payment.qr.account-no:}") String soTaiKhoan,
        @Value("${payment.qr.account-name:}") String tenChuTaiKhoan,
        Clock dongHo) {
        this.donHang = donHang;
        this.maNganHang = maNganHang;
        this.soTaiKhoan = soTaiKhoan;
        this.tenChuTaiKhoan = tenChuTaiKhoan;
        this.dongHo = dongHo;
    }

    public QrPayment layQr(String ma, String email) {
        return taoQr(donHang.layDonDuocPhepTruyCap(ma, email));
    }

    public QrPayment layQrKhach(String ma, Set<String> maDonTrongPhien) {
        return taoQr(donHang.layDonKhachDuocPhepTruyCap(ma, maDonTrongPhien));
    }

    private QrPayment taoQr(Order don) {
        if (!"CHO_THANH_TOAN".equals(don.trangThai()) ||
            (don.hetHanLuc() != null && !don.hetHanLuc().isAfter(LocalDateTime.now(dongHo)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không còn chờ thanh toán");
        }
        if (!maNganHang.matches("[A-Za-z0-9]{2,20}") ||
            !soTaiKhoan.matches("[A-Za-z0-9]{4,19}")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình tài khoản nhận chuyển khoản");
        }
        long tien;
        try {
            tien = don.tongTien().longValueExact();
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số tiền QR phải là VND nguyên");
        }
        if (tien <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Số tiền QR phải lớn hơn 0");
        String noiDung = don.ma();
        String url = "https://img.vietqr.io/image/" + maNganHang + "-" + soTaiKhoan +
            "-qr_only.svg?amount=" + tien + "&addInfo=" + maHoa(noiDung) +
            "&accountName=" + maHoa(tenChuTaiKhoan);
        return new QrPayment(don.tongTien(), noiDung, url);
    }

    private String maHoa(String giaTri) {
        return URLEncoder.encode(giaTri, StandardCharsets.UTF_8);
    }
}
