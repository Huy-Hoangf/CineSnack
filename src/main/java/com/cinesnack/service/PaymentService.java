package com.cinesnack.service;

import com.cinesnack.entity.Order;
import com.cinesnack.entity.Payment;
import com.cinesnack.repository.OrderRepository;
import com.cinesnack.repository.PaymentRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    public record Receipt(
        @JsonProperty("payment") Payment giaoDich,
        @JsonProperty("change") BigDecimal tienThua
    ) {}

    private final OrderRepository khoDonHang;
    private final PaymentRepository khoThanhToan;
    private final InvoiceService dichVuHoaDon;
    private final UserService dichVuNguoiDung;
    private final Clock dongHo;

    public PaymentService(
        OrderRepository khoDonHang,
        PaymentRepository khoThanhToan,
        InvoiceService dichVuHoaDon,
        UserService dichVuNguoiDung,
        Clock dongHo
    ) {
        this.khoDonHang = khoDonHang;
        this.khoThanhToan = khoThanhToan;
        this.dichVuHoaDon = dichVuHoaDon;
        this.dichVuNguoiDung = dichVuNguoiDung;
        this.dongHo = dongHo;
    }

    @Transactional
    public Receipt thanhToanTaiQuay(String maDonHang, BigDecimal tienKhachDua, String thuDienTu) {
        if (!dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu).laNhanVien()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Chỉ nhân viên được xác nhận tiền mặt"
            );
        }
        if (
            tienKhachDua == null ||
            tienKhachDua.signum() < 0 ||
            tienKhachDua.stripTrailingZeros().scale() > 0 ||
            tienKhachDua.compareTo(new BigDecimal("999999999999")) > 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Số tiền khách đưa không hợp lệ"
            );
        }
        Order donHang = khoDonHang.timVaKhoa(maDonHang);
        if (donHang == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy đơn hàng"
        );
        if ("DA_THANH_TOAN".equals(donHang.trangThai())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã được thanh toán");
        }
        if (tienKhachDua.compareTo(donHang.tongTien()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền khách đưa chưa đủ");
        }
        BigDecimal tienThua = tienKhachDua.subtract(donHang.tongTien());
        LocalDateTime hienTai = LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
        if (
            !"CHO_THANH_TOAN".equals(donHang.trangThai()) ||
            (donHang.hetHanLuc() != null && !donHang.hetHanLuc().isAfter(hienTai))
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không còn được thanh toán");
        }
        if (
            !khoDonHang.coTheThanhToan(donHang.maSo(), hienTai) ||
            khoDonHang.tinhTongChiTiet(donHang.maSo()).compareTo(donHang.tongTien()) != 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Chi tiết đơn hàng không còn hợp lệ"
            );
        }
        Payment giaoDich = khoThanhToan.taoThanhToanTienMat(
            donHang.maSo(),
            donHang.tongTien(),
            hienTai
        );
        if (khoDonHang.doiTrangThai(donHang.maSo(), "CHO_THANH_TOAN", "DA_THANH_TOAN") != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã thay đổi");
        }
        khoDonHang.xacNhanChiTietDaThanhToan(donHang.maSo());
        dichVuHoaDon.phatHanh(donHang, hienTai);
        return new Receipt(giaoDich, tienThua);
    }

    @Transactional
    public Payment xacNhanChuyenKhoan(String maDonHang, String maGiaoDich, BigDecimal soTienNhan, String thuDienTu) {
        if (!dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu).laNhanVien()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ nhân viên được xác nhận chuyển khoản");
        }
        if (maGiaoDich == null || !maGiaoDich.matches("[A-Za-z0-9_-]{4,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giao dịch không hợp lệ");
        }
        if (soTienNhan == null || soTienNhan.signum() <= 0 ||
            soTienNhan.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền đã nhận không hợp lệ");
        }
        Order donHang = khoDonHang.timVaKhoa(maDonHang);
        if (donHang == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        if ("DA_THANH_TOAN".equals(donHang.trangThai())) {
            Payment daCo = khoThanhToan.timThanhToanThanhCong(donHang.maSo());
            if (daCo != null && "CHUYEN_KHOAN".equals(daCo.phuongThuc()) &&
                maGiaoDich.equals(daCo.maGiaoDich()) &&
                soTienNhan.compareTo(donHang.tongTien()) == 0 &&
                daCo.soTien().compareTo(donHang.tongTien()) == 0) return daCo;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã được thanh toán");
        }
        if (soTienNhan.compareTo(donHang.tongTien()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chuyển khoản phải đúng tổng tiền đơn hàng");
        }
        LocalDateTime hienTai = LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
        if (!"CHO_THANH_TOAN".equals(donHang.trangThai()) ||
            (donHang.hetHanLuc() != null && !donHang.hetHanLuc().isAfter(hienTai)) ||
            !khoDonHang.coTheThanhToan(donHang.maSo(), hienTai) ||
            khoDonHang.tinhTongChiTiet(donHang.maSo()).compareTo(donHang.tongTien()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không còn được thanh toán");
        }
        Payment giaoDich;
        try {
            giaoDich = khoThanhToan.taoThanhToanChuyenKhoan(donHang.maSo(), donHang.tongTien(), maGiaoDich, hienTai);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã giao dịch đã được sử dụng");
        }
        if (khoDonHang.doiTrangThai(donHang.maSo(), "CHO_THANH_TOAN", "DA_THANH_TOAN") != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã thay đổi");
        }
        khoDonHang.xacNhanChiTietDaThanhToan(donHang.maSo());
        dichVuHoaDon.phatHanh(donHang, hienTai);
        return giaoDich;
    }

    @Transactional(readOnly = true)
    public List<Payment> layDanhSach(String maDonHang, String thuDienTu) {
        var nguoiDung = dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu);
        Order donHang = khoDonHang.timTheoMa(maDonHang);
        dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung, donHang);
        return khoThanhToan.layDanhSach(donHang.maSo());
    }
}
