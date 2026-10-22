package com.cinesnack.service;

import com.cinesnack.dto.AdminCatalog;
import com.cinesnack.dto.Catalog;
import com.cinesnack.repository.AdminCatalogRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCatalogService {

    private final AdminCatalogRepository repo;
    private final UserService users;
    private final Clock clock;

    public AdminCatalogService(AdminCatalogRepository repo, UserService users, Clock clock) {
        this.repo = repo;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Catalog.Movie> movies(String email, int page, int size) {
        requireAdmin(email);
        return repo.movies(page, size);
    }

    @Transactional(readOnly = true)
    public List<Catalog.Show> shows(String email, int page, int size) {
        requireAdmin(email);
        return repo.shows(page, size);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalog.Room> rooms(String email) {
        requireAdmin(email);
        return repo.rooms();
    }

    @Transactional
    public Catalog.Movie saveMovie(Long id, AdminCatalog.MovieRequest request, String email) {
        requireAdmin(email);
        if (id == null) return repo.movie(repo.insertMovie(request));
        Catalog.Movie old = requireMovie(id);
        if (repo.movieHasTickets(id)) throw conflict(
            "Phim đã có lịch sử vé; không thể sửa thông tin phim."
        );
        if (old.soPhut() != request.duration() && repo.movieHasShows(id)) throw conflict(
            "Phim đã có suất chiếu; không thể đổi thời lượng."
        );
        repo.updateMovie(id, request);
        return repo.movie(id);
    }

    @Transactional
    public void deleteMovie(long id, String email) {
        requireAdmin(email);
        requireMovie(id);
        if (repo.movieHasPendingTickets(id)) throw conflict(
            "Phim còn vé chờ thanh toán; chưa thể ngừng chiếu."
        );
        if (repo.movieHasShows(id)) repo.archiveMovie(id);
        else repo.deleteMovie(id);
    }

    @Transactional
    public Catalog.Show saveShow(Long id, AdminCatalog.ShowRequest request, String email) {
        requireAdmin(email);
        Catalog.Movie movie = requireMovie(request.movieId());
        if ("NGUNG_CHIEU".equals(movie.trangThai())) throw conflict("Phim đã ngừng chiếu.");
        Catalog.Show old = null;
        if (id != null) {
            old = requireShow(id);
            if (repo.showHasTickets(id)) throw conflict(
                "Suất chiếu đã có lịch sử vé; không thể sửa."
            );
        }
        if (!repo.lockRoom(request.roomId())) throw conflict(
            "Phòng chiếu không hoạt động hoặc không tồn tại."
        );
        LocalDateTime start = request
            .startsAt()
            .withOffsetSameInstant(ZoneOffset.ofHours(7))
            .toLocalDateTime()
            .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = start.plusMinutes(movie.soPhut());
        if (
            (old == null || !start.equals(old.batDau().toLocalDateTime())) &&
            !start.isAfter(LocalDateTime.now(clock))
        ) throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Giờ bắt đầu mới phải ở tương lai."
        );
        if (
            !"HUY".equals(request.status()) &&
            repo.overlaps(request.roomId(), start, end, id == null ? 0 : id)
        ) throw conflict("Phòng đã có suất chiếu trùng giờ.");
        if (id == null) id = repo.insertShow(request, start, end);
        else repo.updateShow(id, request, start, end);
        return repo.show(id);
    }

    @Transactional
    public void deleteShow(long id, String email) {
        requireAdmin(email);
        requireShow(id);
        if (repo.showHasTickets(id)) throw conflict("Suất chiếu đã có lịch sử vé; không thể xóa.");
        repo.deleteShow(id);
    }

    private void requireAdmin(String email) {
        if (
            !"QUAN_TRI".equals(users.layTaiKhoanHoatDong(email).vaiTro())
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ quản trị được quản lý phim và suất chiếu."
        );
    }

    private Catalog.Movie requireMovie(long id) {
        Catalog.Movie movie = repo.lockMovie(id);
        if (movie == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy phim."
        );
        return movie;
    }

    private Catalog.Show requireShow(long id) {
        Catalog.Show show = repo.lockShow(id);
        if (show == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy suất chiếu."
        );
        return show;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
