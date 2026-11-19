package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.RoomManagement;
import com.cinesnack.catalog.repository.RoomManagementRepository;
import com.cinesnack.catalog.service.RoomManagementService;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RoomManagementServiceTest {

    final RoomManagementRepository repo = mock(RoomManagementRepository.class);
    final UserService users = mock(UserService.class);
    final RoomManagementService service = new RoomManagementService(
        repo,
        users,
        Clock.fixed(
            Instant.parse("2026-11-03T03:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
        )
    );

    @BeforeEach
    void setup() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(
            new User(1, "admin", "", "QUAN_TRI", "HOAT_DONG")
        );
        when(repo.lockRoom(1)).thenReturn(
            new RoomManagement.Room(1, "Phòng 1", "2D", "HOAT_DONG", 0)
        );
    }

    @Test
    void staffCannotSetUpSeats() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(2, "staff", "", "NHAN_VIEN", "HOAT_DONG")
        );
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                service.setup(
                    1,
                    new RoomManagement.SetupRequest(2, 3, "THUONG"),
                    "staff"
                )
            )
                .getStatusCode()
                .value()
        );
        verifyNoInteractions(repo);
    }

    @Test
    void createsOneUniqueCodePerSeat() {
        service.setup(
            1,
            new RoomManagement.SetupRequest(2, 3, "THUONG"),
            "admin"
        );
        for (String code : new String[] { "A1", "A2", "A3", "B1", "B2", "B3" })
            verify(repo).insertSeat(1, code, "THUONG");
    }

    @Test
    void cannotOverwriteAnExistingLayout() {
        when(repo.seatCount(1)).thenReturn(6);
        assertThrows(ResponseStatusException.class, () ->
            service.setup(
                1,
                new RoomManagement.SetupRequest(2, 3, "THUONG"),
                "admin"
            )
        );
        verify(repo, never()).insertSeat(anyLong(), anyString(), anyString());
    }

    @Test
    void cannotDisableRoomWithUpcomingShows() {
        when(repo.hasUpcomingShows(eq(1L), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () ->
            service.save(
                1L,
                new RoomManagement.RoomRequest(
                    "Phòng 1",
                    "2D",
                    "NGUNG_HOAT_DONG"
                ),
                "admin"
            )
        );
        verify(repo, never()).updateRoom(anyLong(), any());
    }

    @Test
    void cannotDeleteRoomWithUpcomingShows() {
        when(repo.hasUpcomingShows(eq(1L), any())).thenReturn(true);
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.delete(1, "admin")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).archiveRoom(anyLong());
        verify(repo, never()).deleteRoom(anyLong());
    }

    @Test
    void archivesRoomWithPastShowsToKeepTicketHistory() {
        when(repo.roomHasShows(1)).thenReturn(true);
        service.delete(1, "admin");
        verify(repo).archiveRoom(1);
        verify(repo, never()).deleteRoom(anyLong());
    }

    @Test
    void deletesRoomAndSeatsWithoutShows() {
        service.delete(1, "admin");
        verify(repo).deleteSeats(1);
        verify(repo).deleteRoom(1);
    }

    @Test
    void restoresArchivedRoomAsInactive() {
        when(repo.restoreRoom(1)).thenReturn(1);
        service.restore(1, "admin");
        verify(repo).restoreRoom(1);
        assertEquals(
            404,
            assertThrows(ResponseStatusException.class, () ->
                service.restore(2, "admin")
            )
                .getStatusCode()
                .value()
        );
    }

    @Test
    void cannotRelabelSeatWithTicketHistory() {
        when(repo.seat(1, 2)).thenReturn(
            new RoomManagement.Seat(2, "A1", "THUONG", "HOAT_DONG")
        );
        when(repo.seatHasTickets(2)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () ->
            service.saveSeat(
                1,
                2,
                new RoomManagement.SeatRequest("B1", "VIP", "HOAT_DONG"),
                "admin"
            )
        );
        verify(repo, never()).updateSeat(anyLong(), anyLong(), any());
    }
}
