package com.cinesnack.payment.repository;

import com.cinesnack.payment.entity.Payment;
import com.cinesnack.common.support.Codes;
import com.cinesnack.common.support.Sql;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    private final JdbcTemplate truyVan;

    public PaymentRepository(JdbcTemplate truyVan) {
        this.truyVan = truyVan;
    }

    public Payment taoThanhToanTienMat(long maDonHang, BigDecimal soTien, LocalDateTime hienTai) {
        String ma = Codes.taoMa("TM");
        long maSo = Sql.themVaLayMa(
            truyVan,
            """
            INSERT INTO THANH_TOAN(don_hang_id, nha_cung_cap, ma_giao_dich, phuong_thuc, so_tien, trang_thai, thanh_toan_luc)
            OUTPUT INSERTED.thanh_toan_id VALUES (?, 'TAI_QUAY', ?, 'TIEN_MAT', ?, 'THANH_CONG', ?)
            """,
            maDonHang,
            ma,
            soTien,
            hienTai
        );
        return new Payment(maSo, maDonHang, ma, "TIEN_MAT", soTien, "THANH_CONG", hienTai);
    }

    public Payment taoThanhToanChuyenKhoan(long maDonHang, BigDecimal soTien, String maGiaoDich, LocalDateTime hienTai) {
        long maSo = Sql.themVaLayMa(truyVan,
            "INSERT INTO THANH_TOAN(don_hang_id,nha_cung_cap,ma_giao_dich,phuong_thuc,so_tien,trang_thai,thanh_toan_luc) OUTPUT INSERTED.thanh_toan_id VALUES(?,'DOI_SOAT_TAI_QUAY',?,'CHUYEN_KHOAN',?,'THANH_CONG',?)",
            maDonHang, maGiaoDich, soTien, hienTai);
        return new Payment(maSo, maDonHang, maGiaoDich, "CHUYEN_KHOAN", soTien, "THANH_CONG", hienTai);
    }

    public List<Payment> layDanhSach(long maDonHang) {
        return truyVan.query(
            "SELECT * FROM THANH_TOAN WHERE don_hang_id = ? ORDER BY thanh_toan_id",
            (dong, viTri) ->
                new Payment(
                    dong.getLong("thanh_toan_id"),
                    dong.getLong("don_hang_id"),
                    dong.getString("ma_giao_dich"),
                    dong.getString("phuong_thuc"),
                    dong.getBigDecimal("so_tien"),
                    dong.getString("trang_thai"),
                    Sql.docThoiGian(dong, "thanh_toan_luc")
                ),
            maDonHang
        );
    }

    public Payment timThanhToanThanhCong(long maDonHang) {
        return layDanhSach(maDonHang)
            .stream()
            .filter(giaoDich -> "THANH_CONG".equals(giaoDich.trangThai()))
            .findFirst()
            .orElse(null);
    }
}
