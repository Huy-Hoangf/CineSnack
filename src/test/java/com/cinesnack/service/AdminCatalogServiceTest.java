package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.repository.AdminCatalogRepository;
import com.cinesnack.catalog.service.AdminCatalogService;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AdminCatalogServiceTest {

    private final AdminCatalogRepository repo = mock(
        AdminCatalogRepository.class
    );
    private final UserService users = mock(UserService.class);
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-10-14T03:00:00Z"),
        ZoneId.of("Asia/Ho_Chi_Minh")
    );
    private final com.cinesnack.catalog.service.TheaterPolicyService policy = mock(com.cinesnack.catalog.service.TheaterPolicyService.class);
    private final AdminCatalogService service = new AdminCatalogService(
        repo,
        users,
        clock,
        policy
    );
    private final Catalog.Movie movie = new Catalog.Movie(
        1,
        "Phim",
        "Hành động",
        "Nội dung",
        "Diễn viên",
        120,
        "T13",
        "",
        "DANG_CHIEU"
    );
    private final AdminCatalog.ShowRequest request =
        new AdminCatalog.ShowRequest(
            1L,
            2L,
            OffsetDateTime.parse("2026-10-15T10:00:00+07:00"),
            new BigDecimal("80000"),
            "SAP_CHIEU"
        );

    @BeforeEach
    void setup() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(
            new User(1, "admin", "hash", "QUAN_TRI", "HOAT_DONG")
        );
        when(repo.lockMovie(1)).thenReturn(movie);
        when(repo.lockRoom(2)).thenReturn(true);
        when(repo.hasActiveSeat(2)).thenReturn(true);
    }

    @Test
    void cannotOpenSalesInRoomWithoutActiveSeats() {
        when(repo.hasActiveSeat(2)).thenReturn(false);
        var error = assertThrows(ResponseStatusException.class, () ->
            service.saveShow(null, request, "admin")
        );
        assertEquals(409, error.getStatusCode().value());
        verify(repo, never()).insertShow(any(), any(), any());
    }

    @Test
    void existingShowInRoomWithoutActiveSeatsCanBeCancelled() {
        when(repo.hasActiveSeat(2)).thenReturn(false);
        when(repo.lockShow(3)).thenReturn(new Catalog.Show(
            3, 1, "Phim", 2, "Phòng", request.startsAt(), request.startsAt().plusHours(2),
            request.price(), "SAP_CHIEU"
        ));
        var cancel = new AdminCatalog.ShowRequest(
            1L, 2L, request.startsAt(), request.price(), "HUY"
        );
        service.saveShow(3L, cancel, "admin");
        verify(repo).updateShow(eq(3L), eq(cancel), any(), any());
    }

    @Test
    void staffCannotReadManagementCatalogEvenWithOldAdminSession() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(
            new User(1, "admin", "hash", "NHAN_VIEN", "HOAT_DONG")
        );
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                service.movies("admin", 0, 20)
            )
                .getStatusCode()
                .value()
        );
        verifyNoInteractions(repo);
    }

    @Test
    void derivesEndTimeFromMovieAndNormalizesVietnamTime() {
        service.saveShow(null, request, "admin");
        verify(repo).insertShow(
            request,
            LocalDateTime.parse("2026-10-15T10:00:00"),
            LocalDateTime.parse("2026-10-15T12:00:00")
        );
    }

    @Test
    void policySnapshotsServerPricesRatherThanClientAmountsAndProtectsSoldShows() {
        when(policy.pricesForShow(2, request.startsAt())).thenReturn(
            new com.cinesnack.catalog.dto.TheaterPolicy.ShowPrices(new BigDecimal("100000"), new BigDecimal("130000")));
        var fromPolicy = new AdminCatalog.ShowRequest(1L, 2L, request.startsAt(), BigDecimal.ONE, BigDecimal.ONE, "SAP_CHIEU", "POLICY");
        service.saveShow(null, fromPolicy, "admin");
        var captured = org.mockito.ArgumentCaptor.forClass(AdminCatalog.ShowRequest.class);
        verify(repo).insertShow(captured.capture(), any(), any());
        assertEquals(new BigDecimal("100000"), captured.getValue().price());
        assertEquals(new BigDecimal("30000"), captured.getValue().vipSurcharge());
        assertEquals("POLICY", captured.getValue().pricingMode());

        when(repo.lockShow(3)).thenReturn(new Catalog.Show(3, 1, "Phim", 2, "Phòng", request.startsAt(), request.startsAt().plusHours(2), request.price(), "SAP_CHIEU"));
        when(repo.showHasTickets(3)).thenReturn(true);
        clearInvocations(policy);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.saveShow(3L, fromPolicy, "admin")).getStatusCode().value());
        verifyNoInteractions(policy);
        verify(repo, never()).updateShow(anyLong(), any(), any(), any());
    }

    @Test
    void overlappingShowIsRejectedBeforeWriting() {
        when(
            repo.overlaps(
                2,
                request.startsAt().toLocalDateTime(),
                request.startsAt().toLocalDateTime().plusMinutes(120),
                0
            )
        ).thenReturn(true);
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.saveShow(null, request, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).insertShow(any(), any(), any());
    }

    @Test
    void cannotMoveShowWithAnyTicketHistory() {
        when(repo.lockShow(3)).thenReturn(
            new Catalog.Show(
                3,
                1,
                "Phim",
                2,
                "Phòng",
                request.startsAt(),
                request.startsAt().plusHours(2),
                request.price(),
                "SAP_CHIEU"
            )
        );
        when(repo.showHasTickets(3)).thenReturn(true);
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.saveShow(3L, request, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).updateShow(anyLong(), any(), any(), any());
    }

    @Test
    void movieWithSchedulesIsArchivedInsteadOfBreakingForeignKeys() {
        when(repo.movieHasShows(1)).thenReturn(true);
        service.deleteMovie(1, "admin");
        verify(repo).archiveMovie(1);
        verify(repo, never()).deleteMovie(anyLong());
    }

    @Test
    void cannotArchiveMovieWithPendingTickets() {
        when(repo.movieHasPendingTickets(1)).thenReturn(true);
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.deleteMovie(1, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).archiveMovie(anyLong());
        verify(repo, never()).deleteMovie(anyLong());
    }

    @Test
    void cannotChangeDurationOfScheduledMovie() {
        when(repo.movieHasShows(1)).thenReturn(true);
        var edit = new AdminCatalog.MovieRequest(
            "Phim",
            "Hành động",
            "Nội dung",
            "Diễn viên",
            130,
            "T13",
            "",
            "DANG_CHIEU"
        );
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.saveMovie(1L, edit, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).updateMovie(anyLong(), any());
    }

    @Test
    void cannotCreatePastShow() {
        var past = new AdminCatalog.ShowRequest(
            1L,
            2L,
            OffsetDateTime.parse("2026-10-13T10:00:00+07:00"),
            request.price(),
            "SAP_CHIEU"
        );
        assertEquals(
            400,
            assertThrows(ResponseStatusException.class, () ->
                service.saveShow(null, past, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).insertShow(any(), any(), any());
    }
}
