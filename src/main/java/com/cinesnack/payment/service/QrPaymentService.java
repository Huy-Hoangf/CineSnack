package com.cinesnack.payment.service;

import com.cinesnack.booking.entity.Order;
import com.cinesnack.booking.service.OrderService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QrPaymentService {

    public record QrPayment(
        @JsonProperty("amount") BigDecimal soTien,
        @JsonProperty("content") String noiDung,
        @JsonProperty("imageUrl") String diaChiAnh,
        @JsonProperty("bankName") String tenNganHang,
        @JsonProperty("accountNo") String soTaiKhoan,
        @JsonProperty("accountName") String tenChuTaiKhoan,
        @JsonProperty("expiresAt") OffsetDateTime hetHanLuc,
        @JsonProperty("serverTime") OffsetDateTime gioServer
    ) {}

    private final OrderService donHang;
    private final String maNganHang;
    private final String soTaiKhoan;
    private final String tenChuTaiKhoan;
    private final String tenNganHang;
    private final Clock dongHo;

    @Autowired
    public QrPaymentService(
        OrderService donHang,
        @Value("${payment.qr.bank-id:}") String maNganHang,
        @Value("${payment.qr.account-no:}") String soTaiKhoan,
        @Value("${payment.qr.account-name:}") String tenChuTaiKhoan,
        @Value("${payment.qr.bank-name:Techcombank}") String tenNganHang,
        Clock dongHo
    ) {
        this.donHang = donHang;
        this.maNganHang = maNganHang;
        this.soTaiKhoan = soTaiKhoan;
        this.tenChuTaiKhoan = tenChuTaiKhoan;
        this.tenNganHang = tenNganHang;
        this.dongHo = dongHo;
    }

    public QrPaymentService(
        OrderService donHang,
        String maNganHang,
        String soTaiKhoan,
        String tenChuTaiKhoan,
        Clock dongHo
    ) {
        this(
            donHang,
            maNganHang,
            soTaiKhoan,
            tenChuTaiKhoan,
            "Techcombank",
            dongHo
        );
    }

    public QrPayment layQr(String ma, String email) {
        return taoQr(donHang.layDonDuocPhepTruyCap(ma, email));
    }

    public QrPayment layQrKhach(String ma, Set<String> maDonTrongPhien) {
        return taoQr(donHang.layDonKhachDuocPhepTruyCap(ma, maDonTrongPhien));
    }

    private QrPayment taoQr(Order don) {
        LocalDateTime now = LocalDateTime.now(dongHo.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        if (
            !"CHO_THANH_TOAN".equals(don.trangThai()) ||
            (don.hetHanLuc() != null &&
                !don.hetHanLuc().isAfter(now))
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Đơn không còn chờ thanh toán"
            );
        }
        if (
            !maNganHang.matches("[A-Za-z0-9]{2,20}") ||
            !soTaiKhoan.matches("[A-Za-z0-9]{4,19}")
        ) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Chưa cấu hình tài khoản nhận chuyển khoản"
            );
        }
        long tien;
        try {
            tien = don.tongTien().longValueExact();
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Số tiền QR phải là VND nguyên"
            );
        }
        if (tien <= 0) throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Số tiền QR phải lớn hơn 0"
        );
        String noiDung = don.ma();
        String url =
            "https://img.vietqr.io/image/" +
            maNganHang +
            "-" +
            soTaiKhoan +
            "-qr_only.svg?amount=" +
            tien +
            "&addInfo=" +
            maHoa(noiDung) +
            "&accountName=" +
            maHoa(tenChuTaiKhoan);
        return new QrPayment(
            don.tongTien(),
            noiDung,
            url,
            tenNganHang,
            soTaiKhoan,
            tenChuTaiKhoan,
            don.hetHanLuc() == null ? null : don.hetHanLuc().atOffset(ZoneOffset.ofHours(7)),
            now.atOffset(ZoneOffset.ofHours(7))
        );
    }

    private String maHoa(String giaTri) {
        return URLEncoder.encode(giaTri, StandardCharsets.UTF_8);
    }
}
