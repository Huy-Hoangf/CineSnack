package com.cinesnack.catalog.repository;

import com.cinesnack.catalog.dto.TheaterPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TheaterPolicyRepository {
    private final JdbcTemplate jdbc;
    public TheaterPolicyRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean lockRoom(long roomId) {
        return !jdbc.query("SELECT phong_id FROM PHONG_CHIEU WITH (UPDLOCK,HOLDLOCK) WHERE phong_id=? AND trang_thai<>'DA_XOA'",
            (r,i)->r.getLong(1), roomId).isEmpty();
    }
    public boolean roomExists(long roomId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM PHONG_CHIEU WHERE phong_id=? AND trang_thai<>'DA_XOA'", Integer.class, roomId) > 0;
    }
    public TheaterPolicy.RoomPricing pricing(long roomId) {
        var rows = jdbc.query("SELECT * FROM BANG_GIA_PHONG WHERE phong_id=?", (r,i)->new TheaterPolicy.RoomPricing(
            r.getBigDecimal("gia_thuong_ngay_thuong"), r.getBigDecimal("gia_vip_ngay_thuong"),
            r.getBigDecimal("gia_thuong_cuoi_tuan"), r.getBigDecimal("gia_vip_cuoi_tuan")), roomId);
        return rows.isEmpty() ? null : rows.get(0);
    }
    // Room row lock held by service serializes initial insert and later writes.
    public void savePricing(long roomId, TheaterPolicy.RoomPricing p) {
        int changed = jdbc.update("UPDATE BANG_GIA_PHONG SET gia_thuong_ngay_thuong=?,gia_vip_ngay_thuong=?,gia_thuong_cuoi_tuan=?,gia_vip_cuoi_tuan=? WHERE phong_id=?",
            p.weekdayRegular(),p.weekdayVip(),p.weekendRegular(),p.weekendVip(),roomId);
        if(changed == 0) jdbc.update("INSERT INTO BANG_GIA_PHONG(phong_id,gia_thuong_ngay_thuong,gia_vip_ngay_thuong,gia_thuong_cuoi_tuan,gia_vip_cuoi_tuan) VALUES(?,?,?,?,?)",
            roomId,p.weekdayRegular(),p.weekdayVip(),p.weekendRegular(),p.weekendVip());
    }
    public TheaterPolicy.Settings policy() {
        return jdbc.queryForObject("SELECT gioi_han_soat_muon_phut FROM CHINH_SACH_RAP WHERE chinh_sach_id=1",
            (r,i)->new TheaterPolicy.Settings((Integer)r.getObject(1)));
    }
    public void savePolicy(TheaterPolicy.Settings p) {
        jdbc.update("UPDATE CHINH_SACH_RAP SET gioi_han_soat_muon_phut=? WHERE chinh_sach_id=1",p.lateCheckInMinutes());
    }
}
