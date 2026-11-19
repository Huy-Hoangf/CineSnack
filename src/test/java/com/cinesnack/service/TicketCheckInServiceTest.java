package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinesnack.account.entity.User;
import com.cinesnack.account.service.UserService;
import com.cinesnack.booking.repository.TicketCheckInRepository;
import com.cinesnack.booking.service.TicketCheckInService;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TicketCheckInServiceTest {

    @Test
    void batchPrintClaimsEachTicketAndRejectsDuplicates() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(new User(7,"staff","","NHAN_VIEN","HOAT_DONG"));
        when(repo.claimPrint(anyString(), eq(7L), any())).thenReturn(1);
        assertEquals(2, service.issueBatch(java.util.List.of("VE1", "VE2"), "staff").size());
        verify(repo).claimPrint(eq("VE1"), eq(7L), any());
        verify(repo).claimPrint(eq("VE2"), eq(7L), any());
        assertThrows(ResponseStatusException.class, () -> service.issueBatch(java.util.List.of("VE1", "ve1"), "staff"));
    }

    final TicketCheckInRepository repo = mock(TicketCheckInRepository.class);
    final UserService users = mock(UserService.class);
    final com.cinesnack.catalog.service.TheaterPolicyService policy = mock(com.cinesnack.catalog.service.TheaterPolicyService.class);
    final TicketCheckInService service = new TicketCheckInService(
        repo,
        users,
        Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC),
        policy
    );

    @org.junit.jupiter.api.BeforeEach
    void setupPolicy() { when(policy.currentPolicy()).thenReturn(new com.cinesnack.catalog.dto.TheaterPolicy.Settings(null)); }

    @Test
    void rechecksCurrentRoleBeforeTouchingTicket() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(7, "staff", "", "KHACH_HANG", "HOAT_DONG")
        );
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                service.checkIn("VE1", "staff")
            )
                .getStatusCode()
                .value()
        );
        verifyNoInteractions(repo);
    }

    @Test
    void recordsVietnamTimeEvenWhenClockUsesUtc() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(7, "staff", "", "NHAN_VIEN", "HOAT_DONG")
        );
        when(repo.checkIn(eq("VE1"), eq(7L), any(), isNull())).thenReturn(1);
        var result = service.checkIn(" VE1 ", "staff");
        assertEquals(
            OffsetDateTime.parse("2026-11-03T19:00:00+07:00"),
            result.checkedAt()
        );
        assertEquals(7, result.staffId());
    }

    @Test
    void passesConfiguredLateLimitToAtomicUpdate() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(new User(7,"staff","","NHAN_VIEN","HOAT_DONG"));
        when(policy.currentPolicy()).thenReturn(new com.cinesnack.catalog.dto.TheaterPolicy.Settings(15));
        when(repo.checkIn(eq("VE1"), eq(7L), any(), eq(15))).thenReturn(1);
        service.checkIn("VE1", "staff");
        verify(repo).checkIn("VE1",7,LocalDateTime.parse("2026-11-03T19:00:00"),15);
    }
    @Test
    void rejectedTicketDoesNotReturnDetails() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(7, "staff", "", "NHAN_VIEN", "HOAT_DONG")
        );
        assertEquals(
            409,
            assertThrows(ResponseStatusException.class, () ->
                service.checkIn("VE1", "staff")
            )
                .getStatusCode()
                .value()
        );
        verify(repo, never()).ticket(anyString());
    }

    @Test
    void onlyStaffCanClaimPrint() {
        when(users.layTaiKhoanHoatDong("admin")).thenReturn(
            new User(9, "admin", "", "QUAN_TRI", "HOAT_DONG")
        );
        assertEquals(
            403,
            assertThrows(ResponseStatusException.class, () ->
                service.issue("VE1", "admin")
            )
                .getStatusCode()
                .value()
        );
        verifyNoInteractions(repo);
    }

    @Test
    void printClaimIsMadeBeforeTicketDetailsAreReturned() {
        when(users.layTaiKhoanHoatDong("staff")).thenReturn(
            new User(7, "staff", "", "NHAN_VIEN", "HOAT_DONG")
        );
        when(repo.claimPrint(eq("VE1"), eq(7L), any())).thenReturn(1);
        var result = service.issue(" VE1 ", "staff");
        assertEquals(
            OffsetDateTime.parse("2026-11-03T19:00:00+07:00"),
            result.printedAt()
        );
        verify(repo).ticket("VE1");
    }
}
