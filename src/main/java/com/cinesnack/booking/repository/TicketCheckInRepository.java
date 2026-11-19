package com.cinesnack.booking.repository;

import com.cinesnack.booking.entity.Ticket;
import com.cinesnack.common.support.Sql;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TicketCheckInRepository {

    private final JdbcTemplate db;

    public TicketCheckInRepository(JdbcTemplate db) {
        this.db = db;
    }

    // A single conditional update prevents two scanners from admitting the same
    // ticket.
    public int checkIn(String code, long staff, LocalDateTime now, Integer lateCheckInMinutes) {
        return db.update(
            """
            UPDATE VE SET trang_thai='DA_SOAT',soat_ve_luc=?,nhan_vien_soat_id=?
            WHERE UPPER(ma_ve)=UPPER(?) AND trang_thai='DA_THANH_TOAN'
            AND EXISTS (SELECT 1 FROM HOA_DON_VE h JOIN DON_HANG d ON d.don_hang_id=h.don_hang_id
                WHERE h.hoa_don_ve_id=VE.hoa_don_ve_id AND d.don_hang_id=VE.don_hang_id
                AND h.trang_thai='DA_THANH_TOAN' AND d.trang_thai='DA_THANH_TOAN')
            AND EXISTS (SELECT 1 FROM SUAT_CHIEU s WHERE s.suat_chieu_id=VE.suat_chieu_id
                AND s.trang_thai IN ('SAP_CHIEU','DANG_CHIEU') AND s.bat_dau<=? AND s.ket_thuc>?
                AND (?=1 OR s.bat_dau>=?))
            """,
            now,
            staff,
            code,
            now.plusMinutes(30),
            now,
            lateCheckInMinutes == null ? 1 : 0,
            now.minusMinutes(lateCheckInMinutes == null ? 0 : lateCheckInMinutes)
        );
    }

    // Claim the sole physical print before opening the browser print dialog.
    public int claimPrint(String code, long staff, LocalDateTime now) {
        return db.update(
            """
            UPDATE VE SET in_ve_luc=?,nhan_vien_in_id=?
            WHERE UPPER(ma_ve)=UPPER(?) AND trang_thai='DA_THANH_TOAN' AND in_ve_luc IS NULL
            AND EXISTS (SELECT 1 FROM HOA_DON_VE h JOIN DON_HANG d ON d.don_hang_id=h.don_hang_id
                WHERE h.hoa_don_ve_id=VE.hoa_don_ve_id AND d.don_hang_id=VE.don_hang_id
                AND h.trang_thai='DA_THANH_TOAN' AND d.trang_thai='DA_THANH_TOAN')
            AND EXISTS (SELECT 1 FROM SUAT_CHIEU s WHERE s.suat_chieu_id=VE.suat_chieu_id
                AND s.trang_thai IN ('SAP_CHIEU','DANG_CHIEU','DA_CHIEU'))
            """,
            now,
            staff,
            code
        );
    }

    public Ticket ticket(String code) {
        var tickets = db.query(
            """
            SELECT v.*,g.ma_ghe,p.ten_phim,r.ten_phong,s.bat_dau FROM VE v
            JOIN GHE g ON g.ghe_id=v.ghe_id JOIN SUAT_CHIEU s ON s.suat_chieu_id=v.suat_chieu_id
            JOIN PHIM p ON p.phim_id=s.phim_id JOIN PHONG_CHIEU r ON r.phong_id=s.phong_id
            WHERE UPPER(v.ma_ve)=UPPER(?)
            """,
            (d, i) ->
                new Ticket(
                    d.getLong("ve_id"),
                    d.getLong("suat_chieu_id"),
                    d.getLong("ghe_id"),
                    d.getString("ma_ghe"),
                    d.getString("ma_ve"),
                    d.getBigDecimal("don_gia"),
                    d.getString("trang_thai"),
                    d.getString("ten_phim"),
                    d.getString("ten_phong"),
                    Sql.docThoiGian(d, "bat_dau").atOffset(
                        ZoneOffset.ofHours(7)
                    ),
                    d.getTimestamp("in_ve_luc") != null
                ),
            code
        );
        return tickets.isEmpty() ? null : tickets.get(0);
    }
}
