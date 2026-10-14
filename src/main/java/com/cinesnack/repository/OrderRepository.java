package com.cinesnack.repository;

import com.cinesnack.entity.*;
import com.cinesnack.support.Codes;
import com.cinesnack.support.Sql;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final JdbcTemplate truyVan;

    public OrderRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public Order timVaKhoa(String ma) {
        return timTheoMa(
            "SELECT * FROM DON_HANG WITH (UPDLOCK, HOLDLOCK) WHERE ma_don_hang = ?",
            ma
        );
    }

    public Order timTheoMa(String ma) {
        return timTheoMa("SELECT * FROM DON_HANG WHERE ma_don_hang = ?", ma);
    }

    private Order timTheoMa(String sql, String ma) {
        var ds = truyVan.query(sql, (dong, viTri) -> docDonHang(dong), ma);
        return ds.isEmpty() ? null : ds.get(0);
    }

    public List<Order> layDanhSach(long maNguoiDung, int trang, int kichThuoc) {
        return truyVan.query(
            "SELECT * FROM DON_HANG WHERE nguoi_dung_id=? ORDER BY don_hang_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            (dong, viTri) -> docDonHang(dong),
            maNguoiDung,
            (long) trang * kichThuoc,
            kichThuoc
        );
    }

    public List<Order> layTatCa(int trang, int kichThuoc) {
        return truyVan.query(
            "SELECT * FROM DON_HANG ORDER BY don_hang_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            (dong, viTri) -> docDonHang(dong),
            (long) trang * kichThuoc,
            kichThuoc
        );
    }

    public long taoDonHang(
        long maNguoiDung,
        String ma,
        String email,
        BigDecimal tong,
        LocalDateTime hetHan,
        LocalDateTime hienTai
    ) {
        return Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO DON_HANG(nguoi_dung_id,ma_don_hang,email_nhan_ve,tong_tien,trang_thai,het_han_luc,tao_luc)
            OUTPUT INSERTED.don_hang_id VALUES(?,?,?,?,'CHO_THANH_TOAN',?,?)
            """,
            maNguoiDung,
            ma,
            email,
            tong,
            hetHan,
            hienTai
        );
    }

    public void taoHoaDonVe(long maDon, BigDecimal tong, String email, LocalDateTime luc) {
        truyVan.update(
            "INSERT INTO HOA_DON_VE(don_hang_id,ma_hoa_don,tong_tien,email_nhan,tao_luc) VALUES(?,?,?,?,?)",
            maDon,
            Codes.taoMa("HDV"),
            tong,
            email,
            luc
        );
    }

    public void taoHoaDonBapNuoc(long maDon, BigDecimal tong, String email, LocalDateTime luc) {
        truyVan.update(
            "INSERT INTO HOA_DON_BONG_NUOC(don_hang_id,ma_hoa_don,tong_tien,email_nhan,tao_luc) VALUES(?,?,?,?,?)",
            maDon,
            Codes.taoMa("HDB"),
            tong,
            email,
            luc
        );
    }

    public boolean gheDaDuocDat(long maSuat, long maGhe) {
        Integer n = truyVan.queryForObject(
            "SELECT COUNT(*) FROM VE WHERE suat_chieu_id=? AND ghe_id=? AND trang_thai IN ('CHO_THANH_TOAN','DA_THANH_TOAN','DA_SOAT')",
            Integer.class,
            maSuat,
            maGhe
        );
        return n != null && n > 0;
    }

    public void themVe(long maDon, long maSuat, long maGhe, BigDecimal gia) {
        int n = truyVan.update(
            """
            INSERT INTO VE(hoa_don_ve_id,suat_chieu_id,ghe_id,phong_id,ma_ve,don_gia,trang_thai)
            SELECT h.hoa_don_ve_id,s.suat_chieu_id,?,s.phong_id,?,?,'CHO_THANH_TOAN'
            FROM HOA_DON_VE h CROSS JOIN SUAT_CHIEU s WHERE h.don_hang_id=? AND s.suat_chieu_id=?
            """,
            maGhe,
            Codes.taoMa("VE"),
            gia,
            maDon,
            maSuat
        );
        if (n != 1) throw new IllegalStateException("Thiếu hóa đơn vé hoặc suất chiếu");
    }

    public void themBapNuoc(
        long maDon,
        Long maSanPham,
        Long maCombo,
        String ten,
        int soLuong,
        BigDecimal gia
    ) {
        int n = truyVan.update(
            """
            INSERT INTO CHI_TIET_HOA_DON_BONG_NUOC(hoa_don_bong_nuoc_id,san_pham_id,combo_id,ten_luc_mua,so_luong,don_gia,trang_thai_phuc_vu)
            SELECT hoa_don_bong_nuoc_id,?,?,?,?,?,'CHO_THANH_TOAN' FROM HOA_DON_BONG_NUOC WHERE don_hang_id=?
            """,
            maSanPham,
            maCombo,
            ten,
            soLuong,
            gia,
            maDon
        );
        if (n != 1) throw new IllegalStateException("Thiếu hóa đơn bắp nước");
    }

    public List<Ticket> layVe(long maDon) {
        return truyVan.query(
            "SELECT v.*,g.ma_ghe FROM VE v JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id JOIN GHE g ON g.ghe_id=v.ghe_id WHERE h.don_hang_id=? ORDER BY v.ve_id",
            (d, i) ->
                new Ticket(
                    d.getLong("ve_id"),
                    d.getLong("suat_chieu_id"),
                    d.getLong("ghe_id"),
                    d.getString("ma_ghe"),
                    d.getString("ma_ve"),
                    d.getBigDecimal("don_gia"),
                    d.getString("trang_thai")
                ),
            maDon
        );
    }

    public List<OrderItem> layBapNuoc(long maDon) {
        return truyVan.query(
            "SELECT c.* FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id WHERE h.don_hang_id=? ORDER BY c.chi_tiet_id",
            (d, i) ->
                new OrderItem(
                    d.getLong("chi_tiet_id"),
                    d.getObject("san_pham_id", Long.class),
                    d.getObject("combo_id", Long.class),
                    d.getString("ten_luc_mua"),
                    d.getInt("so_luong"),
                    d.getBigDecimal("don_gia"),
                    d.getString("trang_thai_phuc_vu")
                ),
            maDon
        );
    }

    public BigDecimal tinhTongChiTiet(long maDon) {
        return truyVan.queryForObject(
            """
            SELECT COALESCE((SELECT SUM(v.don_gia) FROM VE v JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id WHERE h.don_hang_id=?),0)
                 + COALESCE((SELECT SUM(c.so_luong*c.don_gia) FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id WHERE h.don_hang_id=?),0)
            """,
            BigDecimal.class,
            maDon,
            maDon
        );
    }

    public boolean coTheThanhToan(long maDon, LocalDateTime luc) {
        Integer sai = truyVan.queryForObject(
            """
            SELECT
            (SELECT COUNT(*) FROM VE v JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id
             JOIN SUAT_CHIEU s ON s.suat_chieu_id=v.suat_chieu_id JOIN GHE g ON g.ghe_id=v.ghe_id
             JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id JOIN PHIM p ON p.phim_id=s.phim_id
             WHERE h.don_hang_id=? AND (v.trang_thai<>'CHO_THANH_TOAN' OR h.trang_thai<>'CHO_THANH_TOAN' OR s.trang_thai<>'SAP_CHIEU' OR s.bat_dau<=? OR g.phong_id<>s.phong_id OR g.trang_thai<>'HOAT_DONG' OR r.trang_thai<>'HOAT_DONG' OR p.trang_thai<>'DANG_CHIEU'))
            + (SELECT COUNT(*) FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id
               WHERE h.don_hang_id=? AND (c.trang_thai_phuc_vu<>'CHO_THANH_TOAN' OR h.trang_thai<>'CHO_THANH_TOAN'))
            + (SELECT COUNT(*) FROM HOA_DON_VE h WHERE h.don_hang_id=? AND h.tong_tien<>COALESCE((SELECT SUM(v.don_gia) FROM VE v WHERE v.hoa_don_ve_id=h.hoa_don_ve_id),-1))
            + (SELECT COUNT(*) FROM HOA_DON_BONG_NUOC h WHERE h.don_hang_id=? AND h.tong_tien<>COALESCE((SELECT SUM(c.so_luong*c.don_gia) FROM CHI_TIET_HOA_DON_BONG_NUOC c WHERE c.hoa_don_bong_nuoc_id=h.hoa_don_bong_nuoc_id),-1))
            """,
            Integer.class,
            maDon,
            luc,
            maDon,
            maDon,
            maDon
        );
        Integer n = truyVan.queryForObject(
            """
            SELECT (SELECT COUNT(*) FROM VE v JOIN HOA_DON_VE h ON h.hoa_don_ve_id=v.hoa_don_ve_id WHERE h.don_hang_id=?)
                 + (SELECT COUNT(*) FROM CHI_TIET_HOA_DON_BONG_NUOC c JOIN HOA_DON_BONG_NUOC h ON h.hoa_don_bong_nuoc_id=c.hoa_don_bong_nuoc_id WHERE h.don_hang_id=?)
            """,
            Integer.class,
            maDon,
            maDon
        );
        return sai != null && sai == 0 && n != null && n > 0;
    }

    public int doiTrangThai(long maDon, String cu, String moi) {
        return truyVan.update(
            "UPDATE DON_HANG SET trang_thai=? WHERE don_hang_id=? AND trang_thai=?",
            moi,
            maDon,
            cu
        );
    }

    public void xacNhanChiTietDaThanhToan(long maDon) {
        doiChiTiet(maDon, "DA_THANH_TOAN", "CHO_NHAN");
    }

    public void dongChiTiet(long maDon, String trangThai) {
        doiChiTiet(maDon, trangThai, trangThai);
        truyVan.update(
            "UPDATE HOA_DON_VE SET trang_thai=? WHERE don_hang_id=? AND trang_thai='CHO_THANH_TOAN'",
            trangThai,
            maDon
        );
        truyVan.update(
            "UPDATE HOA_DON_BONG_NUOC SET trang_thai=? WHERE don_hang_id=? AND trang_thai='CHO_THANH_TOAN'",
            trangThai,
            maDon
        );
    }

    private void doiChiTiet(long maDon, String ve, String bap) {
        truyVan.update(
            "UPDATE VE SET trang_thai=? WHERE hoa_don_ve_id IN(SELECT hoa_don_ve_id FROM HOA_DON_VE WHERE don_hang_id=?) AND trang_thai='CHO_THANH_TOAN'",
            ve,
            maDon
        );
        truyVan.update(
            "UPDATE CHI_TIET_HOA_DON_BONG_NUOC SET trang_thai_phuc_vu=? WHERE hoa_don_bong_nuoc_id IN(SELECT hoa_don_bong_nuoc_id FROM HOA_DON_BONG_NUOC WHERE don_hang_id=?) AND trang_thai_phuc_vu='CHO_THANH_TOAN'",
            bap,
            maDon
        );
    }

    public List<String> layMaDonHetHan(LocalDateTime luc) {
        return truyVan.queryForList(
            "SELECT TOP(100) ma_don_hang FROM DON_HANG WHERE trang_thai='CHO_THANH_TOAN' AND het_han_luc<=? ORDER BY het_han_luc,don_hang_id",
            String.class,
            luc
        );
    }

    private Order docDonHang(ResultSet d) throws SQLException {
        return new Order(
            d.getLong("don_hang_id"),
            d.getObject("nguoi_dung_id", Long.class),
            d.getString("ma_don_hang"),
            d.getString("email_nhan_ve"),
            d.getBigDecimal("tong_tien"),
            d.getString("trang_thai"),
            Sql.docThoiGian(d, "het_han_luc"),
            Sql.docThoiGian(d, "tao_luc")
        );
    }
}
