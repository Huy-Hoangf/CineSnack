package com.cinesnack.service;

import com.cinesnack.dto.CreateOrderRequest;
import com.cinesnack.entity.*;
import com.cinesnack.repository.BookingCatalogRepository;
import com.cinesnack.repository.OrderRepository;
import com.cinesnack.support.Codes;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    public record Details(
        @JsonProperty("order") Order donHang,
        @JsonProperty("tickets") List<Ticket> dsVe,
        @JsonProperty("snacks") List<OrderItem> dsBapNuoc
    ) {}

    private record FoodLine(
        Long maSanPham,
        Long maCombo,
        BookingCatalogRepository.Food matHang,
        int soLuong
    ) {}

    private final OrderRepository khoDonHang;
    private final BookingCatalogRepository danhMucDatVe;
    private final UserService dichVuNguoiDung;
    private final Clock dongHo;

    public OrderService(
        OrderRepository khoDonHang,
        BookingCatalogRepository danhMucDatVe,
        UserService dichVuNguoiDung,
        Clock dongHo
    ) {
        this.khoDonHang = khoDonHang;
        this.danhMucDatVe = danhMucDatVe;
        this.dichVuNguoiDung = dichVuNguoiDung;
        this.dongHo = dongHo;
    }

    @Transactional
    public Order taoDonHang(CreateOrderRequest yeuCau, String thuDienTu) {
        User nguoiDung = dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu);
        kiemTraLuaChon(yeuCau);
        var suatChieu = yeuCau.dsMaGhe().isEmpty()
            ? null
            : danhMucDatVe.timVaKhoaSuatChieu(yeuCau.maSuatChieu());
        LocalDateTime hienTai = LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime hetHan = hienTai.plusMinutes(15);
        BigDecimal giaVe = BigDecimal.ZERO;

        if (!yeuCau.dsMaGhe().isEmpty()) {
            if (
                suatChieu == null ||
                !"SAP_CHIEU".equals(suatChieu.trangThai()) ||
                !suatChieu.batDau().isAfter(hienTai)
            ) {
                throw loiXungDot("Suất chiếu không còn nhận đặt vé");
            }
            if (suatChieu.batDau().isBefore(hetHan)) hetHan = suatChieu.batDau();
            giaVe = suatChieu.gia();
            for (long maGhe : yeuCau.dsMaGhe()) {
                var ghe = danhMucDatVe.timGhe(maGhe);
                if (
                    ghe == null ||
                    ghe.maPhong() != suatChieu.maPhong() ||
                    !"HOAT_DONG".equals(ghe.trangThai())
                ) {
                    throw loiDuLieu("Ghế không hợp lệ hoặc không thuộc phòng chiếu");
                }
                if (khoDonHang.gheDaDuocDat(suatChieu.maSo(), maGhe)) throw loiXungDot(
                    "Ghế đã được giữ hoặc bán"
                );
            }
        }

        List<FoodLine> dsMatHang = new ArrayList<>();
        for (var luaChon : yeuCau.dsBapNuoc()) {
            dsMatHang.add(
                taoDongBapNuoc(
                    luaChon.maSanPham(),
                    null,
                    danhMucDatVe.timSanPham(luaChon.maSanPham()),
                    luaChon.soLuong()
                )
            );
        }
        for (var luaChon : yeuCau.dsCombo()) {
            dsMatHang.add(
                taoDongBapNuoc(
                    null,
                    luaChon.maCombo(),
                    danhMucDatVe.timCombo(luaChon.maCombo()),
                    luaChon.soLuong()
                )
            );
        }
        BigDecimal tongTien = giaVe.multiply(BigDecimal.valueOf(yeuCau.dsMaGhe().size()));
        for (var dongChiTiet : dsMatHang)
            tongTien = tongTien.add(
                dongChiTiet.matHang().gia().multiply(BigDecimal.valueOf(dongChiTiet.soLuong()))
            );
        if (tongTien.compareTo(new BigDecimal("9999999999.99")) > 0) throw loiDuLieu(
            "Tổng tiền vượt giới hạn"
        );

        String maDonHang = Codes.taoMa("DH");
        long maSo = khoDonHang.taoDonHang(
            nguoiDung.maSo(),
            maDonHang,
            yeuCau.thuDienTu(),
            tongTien,
            hetHan,
            hienTai
        );
        BigDecimal tienVe = giaVe.multiply(BigDecimal.valueOf(yeuCau.dsMaGhe().size()));
        if (!yeuCau.dsMaGhe().isEmpty()) khoDonHang.taoHoaDonVe(
            maSo,
            tienVe,
            yeuCau.thuDienTu(),
            hienTai
        );
        if (!dsMatHang.isEmpty()) khoDonHang.taoHoaDonBapNuoc(
            maSo,
            tongTien.subtract(tienVe),
            yeuCau.thuDienTu(),
            hienTai
        );
        for (long maGhe : yeuCau.dsMaGhe())
            khoDonHang.themVe(maSo, yeuCau.maSuatChieu(), maGhe, giaVe);
        for (var dongChiTiet : dsMatHang)
            khoDonHang.themBapNuoc(
                maSo,
                dongChiTiet.maSanPham(),
                dongChiTiet.maCombo(),
                dongChiTiet.matHang().ten(),
                dongChiTiet.soLuong(),
                dongChiTiet.matHang().gia()
            );
        return new Order(
            maSo,
            nguoiDung.maSo(),
            maDonHang,
            yeuCau.thuDienTu(),
            tongTien,
            "CHO_THANH_TOAN",
            hetHan,
            hienTai
        );
    }

    @Transactional(readOnly = true)
    public Details layChiTiet(String maDonHang, String thuDienTu) {
        Order donHang = layDonDuocPhepTruyCap(maDonHang, thuDienTu);
        return new Details(
            donHang,
            khoDonHang.layVe(donHang.maSo()),
            khoDonHang.layBapNuoc(donHang.maSo())
        );
    }

    public Order layDonDuocPhepTruyCap(String maDonHang, String thuDienTu) {
        User nguoiDung = dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu);
        Order donHang = khoDonHang.timTheoMa(maDonHang);
        dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung, donHang);
        return donHang;
    }

    public List<Order> layDanhSach(String thuDienTu, int trang, int kichThuoc) {
        return khoDonHang.layDanhSach(
            dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu).maSo(),
            trang,
            kichThuoc
        );
    }

    public List<Order> layDanhSachQuanTri(String email, int trang, int kichThuoc) {
        if (
            !dichVuNguoiDung.layTaiKhoanHoatDong(email).laNhanVien()
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ nhân viên được tra cứu toàn rạp"
        );
        return khoDonHang.layTatCa(trang, kichThuoc);
    }

    @Transactional
    public Order huyDonHang(String maDonHang, String thuDienTu) {
        User nguoiDung = dichVuNguoiDung.layTaiKhoanHoatDong(thuDienTu);
        Order donHang = khoDonHang.timVaKhoa(maDonHang);
        dichVuNguoiDung.kiemTraQuyenTruyCapDon(nguoiDung, donHang);
        if (
            "HUY".equals(donHang.trangThai()) || "HET_HAN".equals(donHang.trangThai())
        ) return donHang;
        if (!"CHO_THANH_TOAN".equals(donHang.trangThai())) throw loiXungDot(
            "Chỉ được hủy đơn chưa thanh toán"
        );
        LocalDateTime hienTai = LocalDateTime.now(dongHo).truncatedTo(ChronoUnit.SECONDS);
        String trangThai =
            donHang.hetHanLuc() != null && !donHang.hetHanLuc().isAfter(hienTai)
                ? "HET_HAN"
                : "HUY";
        if (
            khoDonHang.doiTrangThai(donHang.maSo(), "CHO_THANH_TOAN", trangThai) != 1
        ) throw loiXungDot("Đơn hàng đã thay đổi");
        khoDonHang.dongChiTiet(donHang.maSo(), trangThai);
        return new Order(
            donHang.maSo(),
            donHang.maNguoiDung(),
            donHang.ma(),
            donHang.thuDienTu(),
            donHang.tongTien(),
            trangThai,
            donHang.hetHanLuc(),
            donHang.taoLuc()
        );
    }

    private void kiemTraLuaChon(CreateOrderRequest yeuCau) {
        if (
            yeuCau.dsMaGhe().isEmpty() && yeuCau.dsBapNuoc().isEmpty() && yeuCau.dsCombo().isEmpty()
        ) {
            throw loiDuLieu("Đơn hàng phải có vé hoặc bắp nước");
        }
        if (yeuCau.dsMaGhe().isEmpty() != (yeuCau.maSuatChieu() == null)) {
            throw loiDuLieu("Suất chiếu và danh sách ghế phải được chọn cùng nhau");
        }
        if (new HashSet<>(yeuCau.dsMaGhe()).size() != yeuCau.dsMaGhe().size()) {
            throw loiDuLieu("Không được gửi ghế trùng nhau");
        }
        Set<Long> dsMaSanPham = new HashSet<>();
        for (var luaChon : yeuCau.dsBapNuoc()) {
            if (luaChon == null) throw loiDuLieu("Sản phẩm không hợp lệ");
            Long maSanPham = luaChon.maSanPham();
            if (maSanPham == null || !dsMaSanPham.add(maSanPham)) {
                throw loiDuLieu("Mã sản phẩm thiếu hoặc bị trùng");
            }
        }
        Set<Long> dsMaCombo = new HashSet<>();
        for (var luaChon : yeuCau.dsCombo()) {
            if (luaChon == null) throw loiDuLieu("Combo không hợp lệ");
            Long maCombo = luaChon.maCombo();
            if (maCombo == null || !dsMaCombo.add(maCombo)) {
                throw loiDuLieu("Mã combo thiếu hoặc bị trùng");
            }
        }
    }

    private FoodLine taoDongBapNuoc(
        Long maSanPham,
        Long maCombo,
        BookingCatalogRepository.Food matHang,
        int soLuong
    ) {
        if (matHang == null || !"HOAT_DONG".equals(matHang.trangThai())) throw loiXungDot(
            "Sản phẩm hoặc combo không còn bán"
        );
        return new FoodLine(maSanPham, maCombo, matHang, soLuong);
    }

    private ResponseStatusException loiDuLieu(String thongBao) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, thongBao);
    }

    private ResponseStatusException loiXungDot(String thongBao) {
        return new ResponseStatusException(HttpStatus.CONFLICT, thongBao);
    }
}
