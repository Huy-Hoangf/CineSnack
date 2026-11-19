package com.cinesnack.catalog.service;

import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.RoomManagement.*;
import com.cinesnack.catalog.repository.RoomManagementRepository;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomManagementService {

    private final RoomManagementRepository repo;
    private final UserService users;
    private final Clock clock;

    public RoomManagementService(
        RoomManagementRepository repo,
        UserService users,
        Clock clock
    ) {
        this.repo = repo;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Room> rooms(int page, int size, String email) {
        requireAdmin(email);
        return repo.rooms(page, size);
    }

    @Transactional(readOnly = true)
    public List<Room> archivedRooms(int page, int size, String email) {
        requireAdmin(email);
        return repo.archivedRooms(page, size);
    }

    @Transactional
    public void restore(long id, String email) {
        requireAdmin(email);
        if (repo.restoreRoom(id) != 1) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy phòng đã lưu trữ."
        );
    }

    @Transactional
    public long save(Long id, RoomRequest request, String email) {
        requireAdmin(email);
        if (id == null) return repo.insertRoom(request);
        Room old = requireRoom(id);
        if (
            !"HOAT_DONG".equals(request.status()) &&
            repo.hasUpcomingShows(id, now())
        ) throw conflict(
            "Phòng còn lịch chiếu chưa kết thúc; chưa thể ngừng hoạt động."
        );
        if (
            (!old.name().equals(request.name().trim()) ||
                !old.type().equals(request.type().trim())) &&
            repo.roomHasTickets(id)
        ) throw conflict(
            "Phòng đã có lịch sử vé; không thể đổi tên hoặc loại phòng."
        );
        repo.updateRoom(id, request);
        return id;
    }

    @Transactional
    public void delete(long id, String email) {
        requireAdmin(email);
        requireRoom(id);
        if (repo.hasUpcomingShows(id, now())) throw conflict(
            "Phòng còn lịch chiếu chưa kết thúc; chưa thể xóa."
        );
        if (repo.roomHasShows(id)) repo.archiveRoom(id);
        else {
            repo.deleteSeats(id);
            repo.deleteRoom(id);
        }
    }

    @Transactional(readOnly = true)
    public List<Seat> seats(long room, String email) {
        requireAdmin(email);
        return repo.seats(room);
    }

    @Transactional
    public List<Seat> setup(long room, SetupRequest request, String email) {
        requireAdmin(email);
        Room current = requireRoom(room);
        if (!"HOAT_DONG".equals(current.status())) throw conflict(
            "Hãy bật hoạt động phòng trước khi thiết lập ghế."
        );
        if (repo.seatCount(room) > 0) throw conflict(
            "Phòng đã có sơ đồ ghế; hãy sửa từng ghế để giữ nguyên dữ liệu."
        );
        if (repo.hasUpcomingShows(room, now())) throw conflict(
            "Phòng còn lịch chiếu; chưa thể thiết lập lại ghế."
        );
        for (int row = 0; row < request.rows(); row++) for (
            int col = 1;
            col <= request.columns();
            col++
        ) repo.insertSeat(
            room,
            String.valueOf((char) ('A' + row)) + col,
            request.type()
        );
        return repo.seats(room);
    }

    @Transactional
    public Seat saveSeat(
        long room,
        long id,
        SeatRequest request,
        String email
    ) {
        requireAdmin(email);
        requireRoom(room);
        Seat old = repo.seat(room, id);
        if (old == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy ghế trong phòng."
        );
        if (
            !old.code().equals(request.code()) ||
            !old.type().equals(request.type()) ||
            !old.status().equals(request.status())
        ) {
            if (repo.seatHasTickets(id)) throw conflict(
                "Ghế đã có lịch sử vé; không thể thay đổi."
            );
            if (repo.hasUpcomingShows(room, now())) throw conflict(
                "Phòng còn lịch chiếu; chưa thể thay đổi ghế."
            );
        }
        repo.updateSeat(room, id, request);
        return new Seat(id, request.code(), request.type(), request.status());
    }

    private void requireAdmin(String email) {
        if (
            !"QUAN_TRI".equals(users.layTaiKhoanHoatDong(email).vaiTro())
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ quản trị được quản lý phòng và ghế."
        );
    }

    private Room requireRoom(long id) {
        Room room = repo.lockRoom(id);
        if (room == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy phòng chiếu."
        );
        return room;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
