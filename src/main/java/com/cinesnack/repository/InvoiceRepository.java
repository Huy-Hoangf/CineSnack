package com.cinesnack.repository;

import com.cinesnack.entity.Invoice;
import com.cinesnack.entity.Order;
import com.cinesnack.support.Codes;
import com.cinesnack.support.Sql;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepository {

    private final JdbcTemplate truyVan;

    public InvoiceRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public void taoHoaDon(Order donHang, LocalDateTime luc) {
        // Chụp metadata vé tại thời điểm phát hành, không đọc lại danh mục khi xem hóa
        // đơn.
        var dsVe = truyVan.query(
                """
                        SELECT v.ve_id,p.ten_phim,r.ten_phong,g.ma_ghe,s.bat_dau FROM VE v
                        JOIN SUAT_CHIEU s ON s.suat_chieu_id=v.suat_chieu_id
                        JOIN PHIM p ON p.phim_id=s.phim_id JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id JOIN GHE g ON g.ghe_id=v.ghe_id
                        WHERE v.don_hang_id=?
                        """,
                (d, i) -> new Object[] {
                        d.getLong("ve_id"),
                        d.getString("ten_phim") +
                                " | " +
                                d.getString("ten_phong") +
                                " | " +
                                d.getString("ma_ghe") +
                                " | " +
                                Sql.docThoiGian(d, "bat_dau").format(
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                },
                donHang.maSo());
        for (var ve : dsVe)
            truyVan.update("UPDATE VE SET mo_ta_luc_mua=? WHERE ve_id=?", ve[1], ve[0]);
        var tienVe = truyVan.queryForObject(
                "SELECT SUM(don_gia) FROM VE WHERE don_hang_id=?", java.math.BigDecimal.class, donHang.maSo());
        var tienBap = truyVan.queryForObject(
                "SELECT SUM(so_luong*don_gia) FROM CHI_TIET_HOA_DON_BONG_NUOC WHERE don_hang_id=?",
                java.math.BigDecimal.class, donHang.maSo());
        if (tienVe != null) {
            truyVan.update(
                    "INSERT INTO HOA_DON_VE(don_hang_id,ma_hoa_don,tong_tien,trang_thai,email_nhan,tao_luc,phat_hanh_luc) VALUES(?,?,?,'DA_THANH_TOAN',?,?,?)",
                    donHang.maSo(), Codes.taoMa("HDV"), tienVe, donHang.thuDienTu(), luc, luc);
            long id = truyVan.queryForObject("SELECT hoa_don_ve_id FROM HOA_DON_VE WHERE don_hang_id=?", Long.class,
                    donHang.maSo());
            truyVan.update("UPDATE VE SET hoa_don_ve_id=? WHERE don_hang_id=?", id, donHang.maSo());
        }
        if (tienBap != null) {
            truyVan.update(
                    "INSERT INTO HOA_DON_BONG_NUOC(don_hang_id,ma_hoa_don,tong_tien,trang_thai,email_nhan,tao_luc,phat_hanh_luc) VALUES(?,?,?,'DA_THANH_TOAN',?,?,?)",
                    donHang.maSo(), Codes.taoMa("HDB"), tienBap, donHang.thuDienTu(), luc, luc);
            long id = truyVan.queryForObject("SELECT hoa_don_bong_nuoc_id FROM HOA_DON_BONG_NUOC WHERE don_hang_id=?",
                    Long.class, donHang.maSo());
            truyVan.update("UPDATE CHI_TIET_HOA_DON_BONG_NUOC SET hoa_don_bong_nuoc_id=? WHERE don_hang_id=?", id,
                    donHang.maSo());
        }
        if (tienVe == null && tienBap == null)
            throw new IllegalStateException("Đơn không có chi tiết");
    }

    public List<Invoice> timTheoDonHang(long maDon) {
        var ds = new ArrayList<Invoice>();
        for (String loai : List.of("VE", "BONG_NUOC")) {
            // Tên bảng/cột lấy từ tập cố định nội bộ, không nhận từ request.
            String bang = "VE".equals(loai) ? "HOA_DON_VE" : "HOA_DON_BONG_NUOC";
            String cot = "VE".equals(loai) ? "hoa_don_ve_id" : "hoa_don_bong_nuoc_id";
            var headers = truyVan.query(
                    "SELECT * FROM " + bang + " WHERE don_hang_id=? AND trang_thai='DA_THANH_TOAN'",
                    (d, i) -> new Invoice(
                            d.getLong(cot),
                            d.getLong("don_hang_id"),
                            d.getString("ma_hoa_don"),
                            loai,
                            d.getString("email_nhan"),
                            d.getBigDecimal("tong_tien"),
                            Sql.docThoiGian(d, "phat_hanh_luc"),
                            List.of()),
                    maDon);
            for (var h : headers) {
                List<Invoice.Line> dong;
                if ("VE".equals(loai))
                    dong = truyVan.query(
                            "SELECT mo_ta_luc_mua,don_gia FROM VE WHERE hoa_don_ve_id=? ORDER BY ve_id",
                            (d, i) -> new Invoice.Line(
                                    "VE",
                                    d.getString("mo_ta_luc_mua"),
                                    1,
                                    d.getBigDecimal("don_gia")),
                            h.maSo());
                else
                    dong = truyVan.query(
                            "SELECT combo_id,ten_luc_mua,so_luong,don_gia FROM CHI_TIET_HOA_DON_BONG_NUOC WHERE hoa_don_bong_nuoc_id=? ORDER BY chi_tiet_id",
                            (d, i) -> new Invoice.Line(
                                    d.getObject("combo_id") == null ? "SAN_PHAM" : "COMBO",
                                    d.getString("ten_luc_mua"),
                                    d.getInt("so_luong"),
                                    d.getBigDecimal("don_gia")),
                            h.maSo());
                ds.add(
                        new Invoice(
                                h.maSo(),
                                h.maDonHang(),
                                h.ma(),
                                loai,
                                h.thuDienTu(),
                                h.tongTien(),
                                h.phatHanhLuc(),
                                dong));
            }
        }
        return List.copyOf(ds);
    }
}
