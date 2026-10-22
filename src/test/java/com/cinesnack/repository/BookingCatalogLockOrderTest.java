package com.cinesnack.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class BookingCatalogLockOrderTest {

    @Test
    void locksMovieBeforeShowWhenBookingSeats() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(List.of(9L), List.of(9L), List.of(
            new BookingCatalogRepository.Show(
                4,
                2,
                new BigDecimal("75000"),
                LocalDateTime.parse("2026-10-21T10:00:00"),
                "SAP_CHIEU"
            )
        )).when(jdbc).query(
            anyString(),
            org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
            any(Object[].class)
        );

        assertNotNull(new BookingCatalogRepository(jdbc).timVaKhoaSuatChieu(4));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(3)).query(
            sql.capture(),
            org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
            any(Object[].class)
        );
        assertTrue(sql.getAllValues().get(0).contains("SELECT phim_id FROM SUAT_CHIEU"));
        assertTrue(sql.getAllValues().get(1).contains("FROM PHIM WITH (HOLDLOCK)"));
        assertTrue(sql.getAllValues().get(2).contains("SUAT_CHIEU WITH (UPDLOCK, HOLDLOCK)"));
    }
}
