package com.cinesnack.catalog.repository;

import com.cinesnack.catalog.dto.RoomManagement.*;
import com.cinesnack.common.support.Sql;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomManagementRepository {

    private final JdbcTemplate db;

    public RoomManagementRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Room> rooms(int page, int size) {
        return db.query(
            """
            SELECT r.*,(SELECT COUNT(*) FROM GHE g WHERE g.phong_id=r.phong_id) so_ghe
            FROM PHONG_CHIEU r WHERE r.trang_thai<>'DA_XOA'
            ORDER BY r.phong_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """,
            (r, i) ->
                new Room(
                    r.getLong("phong_id"),
                    r.getString("ten_phong"),
                    r.getString("loai_phong"),
                    r.getString("trang_thai"),
                    r.getInt("so_ghe")
                ),
            (long) page * size,
            size
        );
    }

    public List<Room> archivedRooms(int page, int size) {
        return db.query(
            """
            SELECT r.*,(SELECT COUNT(*) FROM GHE g WHERE g.phong_id=r.phong_id) so_ghe
            FROM PHONG_CHIEU r WHERE r.trang_thai='DA_XOA'
            ORDER BY r.phong_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """,
            (r, i) ->
                new Room(
                    r.getLong("phong_id"),
                    r.getString("ten_phong"),
                    r.getString("loai_phong"),
                    r.getString("trang_thai"),
                    r.getInt("so_ghe")
                ),
            (long) page * size,
            size
        );
    }

    public int restoreRoom(long id) {
        return db.update(
            "UPDATE PHONG_CHIEU SET trang_thai='NGUNG_HOAT_DONG' WHERE phong_id=? AND trang_thai='DA_XOA'",
            id
        );
    }

    public Room lockRoom(long id) {
        var rows = db.query(
            "SELECT * FROM PHONG_CHIEU WITH (XLOCK,HOLDLOCK) WHERE phong_id=? AND trang_thai<>'DA_XOA'",
            (r, i) ->
                new Room(
                    r.getLong("phong_id"),
                    r.getString("ten_phong"),
                    r.getString("loai_phong"),
                    r.getString("trang_thai"),
                    0
                ),
            id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long insertRoom(RoomRequest request) {
        return Sql.themVaLayMa(
            db,
            "INSERT INTO PHONG_CHIEU(ten_phong,loai_phong,trang_thai) OUTPUT INSERTED.phong_id VALUES(?,?,?)",
            request.name().trim(),
            request.type().trim(),
            request.status()
        );
    }

    public void updateRoom(long id, RoomRequest request) {
        db.update(
            "UPDATE PHONG_CHIEU SET ten_phong=?,loai_phong=?,trang_thai=? WHERE phong_id=?",
            request.name().trim(),
            request.type().trim(),
            request.status(),
            id
        );
    }

    public boolean hasUpcomingShows(long room, LocalDateTime now) {
        return (
            db.queryForObject(
                "SELECT COUNT(*) FROM SUAT_CHIEU WHERE phong_id=? AND ket_thuc>? AND trang_thai IN ('SAP_CHIEU','DANG_CHIEU')",
                Integer.class,
                room,
                now
            ) > 0
        );
    }

    public boolean roomHasTickets(long room) {
        return (
            db.queryForObject(
                "SELECT COUNT(*) FROM VE WHERE phong_id=?",
                Integer.class,
                room
            ) > 0
        );
    }

    public boolean roomHasShows(long room) {
        return (
            db.queryForObject(
                "SELECT COUNT(*) FROM SUAT_CHIEU WHERE phong_id=?",
                Integer.class,
                room
            ) > 0
        );
    }

    public void archiveRoom(long room) {
        db.update(
            "UPDATE PHONG_CHIEU SET trang_thai='DA_XOA' WHERE phong_id=?",
            room
        );
    }

    public void deleteSeats(long room) {
        db.update("DELETE FROM GHE WHERE phong_id=?", room);
    }

    public void deleteRoom(long room) {
        db.update("DELETE FROM PHONG_CHIEU WHERE phong_id=?", room);
    }

    public int seatCount(long room) {
        return db.queryForObject(
            "SELECT COUNT(*) FROM GHE WHERE phong_id=?",
            Integer.class,
            room
        );
    }

    public List<Seat> seats(long room) {
        return db.query(
            "SELECT * FROM GHE WHERE phong_id=? ORDER BY ma_ghe",
            (r, i) ->
                new Seat(
                    r.getLong("ghe_id"),
                    r.getString("ma_ghe"),
                    r.getString("loai_ghe"),
                    r.getString("trang_thai")
                ),
            room
        );
    }

    public Seat seat(long room, long id) {
        return seats(room)
            .stream()
            .filter(s -> s.id() == id)
            .findFirst()
            .orElse(null);
    }

    public boolean seatHasTickets(long id) {
        return (
            db.queryForObject(
                "SELECT COUNT(*) FROM VE WHERE ghe_id=?",
                Integer.class,
                id
            ) > 0
        );
    }

    public void insertSeat(long room, String code, String type) {
        db.update(
            "INSERT INTO GHE(phong_id,ma_ghe,loai_ghe,trang_thai) VALUES(?,?,?,'HOAT_DONG')",
            room,
            code,
            type
        );
    }

    public void updateSeat(long room, long id, SeatRequest request) {
        db.update(
            "UPDATE GHE SET ma_ghe=?,loai_ghe=?,trang_thai=? WHERE phong_id=? AND ghe_id=?",
            request.code(),
            request.type(),
            request.status(),
            room,
            id
        );
    }
}
