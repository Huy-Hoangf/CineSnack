package com.cinesnack.catalog.service;

import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.repository.AdminCatalogRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCatalogService {

    private final AdminCatalogRepository repo;
    private final UserService users;
    private final Clock clock;
    private final TheaterPolicyService policy;

    public AdminCatalogService(
        AdminCatalogRepository repo,
        UserService users,
        Clock clock,
        TheaterPolicyService policy
    ) {
        this.repo = repo;
        this.users = users;
        this.clock = clock;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public List<Catalog.Movie> movies(String email, int page, int size) {
        kiemTraQuyenQuanTri(email);
        return repo.movies(page, size);
    }

    @Transactional(readOnly = true)
    public List<Catalog.Show> shows(String email, int page, int size) {
        kiemTraQuyenQuanTri(email);
        return repo.shows(page, size);
    }

    @Transactional(readOnly = true)
    public List<Catalog.Show> timeline(String email, LocalDate date, Long roomId, String status) {
        kiemTraQuyenQuanTri(email);
        String filter = status == null ? "" : status;
        if (date == null || (roomId != null && roomId <= 0) ||
            !(filter.isEmpty() || List.of("SAP_CHIEU", "DANG_CHIEU", "DA_CHIEU", "HUY").contains(filter))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày, phòng hoặc trạng thái không hợp lệ.");
        }
        // SQL stores theater-local datetime; each requested date is a Vietnam calendar day.
        return repo.timeline(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), roomId, filter);
    }

    @Transactional(readOnly = true)
    public List<AdminCatalog.Room> rooms(String email) {
        kiemTraQuyenQuanTri(email);
        return repo.rooms();
    }

    @Transactional
    public Catalog.Movie saveMovie(
        Long id,
        AdminCatalog.MovieRequest request,
        String email
    ) {
        kiemTraQuyenQuanTri(email);
        if (id == null) return repo.movie(repo.insertMovie(request));
        Catalog.Movie old = requireMovie(id);
        if (repo.movieHasTickets(id) && (
            !Objects.equals(old.ten(), request.name().trim()) ||
            !Objects.equals(old.theLoai(), request.genre().trim()) ||
            old.soPhut() != request.duration() ||
            !Objects.equals(old.doTuoi(), request.ageRating()) ||
            !Objects.equals(old.trangThai(), request.status())
        )) throw conflict("Phim đã có vé; chỉ được sửa mô tả, diễn viên, poster, đạo diễn và trailer.");
        if (
            old.soPhut() != request.duration() && repo.movieHasShows(id)
        ) throw conflict("Phim đã có suất chiếu; không thể đổi thời lượng.");
        repo.updateMovie(id, request);
        return repo.movie(id);
    }

    @Transactional
    public void deleteMovie(long id, String email) {
        kiemTraQuyenQuanTri(email);
        requireMovie(id);
        if (repo.movieHasPendingTickets(id)) throw conflict(
            "Phim còn vé chờ thanh toán; chưa thể ngừng chiếu."
        );
        if (repo.movieHasShows(id)) repo.archiveMovie(id);
        else repo.deleteMovie(id);
    }

    @Transactional
    public Catalog.Show saveShow(
        Long id,
        AdminCatalog.ShowRequest request,
        String email
    ) {
        kiemTraQuyenQuanTri(email);
        Catalog.Movie movie = requireMovie(request.movieId());
        if ("NGUNG_CHIEU".equals(movie.trangThai())) throw conflict(
            "Phim đã ngừng chiếu."
        );
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
        if ("SAP_CHIEU".equals(request.status()) && !repo.hasActiveSeat(request.roomId())) {
            throw conflict(
                "Phòng chưa có ghế hoạt động. Hãy thiết lập ghế trước khi mở bán suất chiếu."
            );
        }
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
        if ("POLICY".equals(request.pricingMode())) {
            var prices = policy.pricesForShow(request.roomId(), request.startsAt());
            request = new AdminCatalog.ShowRequest(request.movieId(),request.roomId(),request.startsAt(),
                prices.regular(),prices.vip().subtract(prices.regular()),request.status(),"POLICY");
        }
        if (id == null) id = repo.insertShow(request, start, end);
        else repo.updateShow(id, request, start, end);
        return repo.show(id);
    }

    @Transactional
    public void deleteShow(long id, String email) {
        kiemTraQuyenQuanTri(email);
        requireShow(id);
        if (repo.showHasTickets(id)) throw conflict(
            "Suất chiếu đã có lịch sử vé; không thể xóa."
        );
        repo.deleteShow(id);
    }

    public void kiemTraQuyenQuanTri(String email) {
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
