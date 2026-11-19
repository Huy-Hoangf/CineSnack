package com.cinesnack.catalog.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class AdminReportRepositoryTest {
    @Test
    void fillsMissingDaysAndKeepsDailyCompatible() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        LocalDate day = LocalDate.of(2026, 11, 19);
        var paid = new AdminReportRepository.Daily(day, 1, 2, 3,
            new BigDecimal("160000"), new BigDecimal("90000"), new BigDecimal("250000"));
        when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(paid));
        var repo = new AdminReportRepository(db);
        var range = repo.range(day.minusDays(1), day.plusDays(1));
        assertEquals(3, range.size());
        assertEquals(day.minusDays(1), range.get(0).date());
        assertEquals(BigDecimal.ZERO, range.get(0).totalRevenue());
        assertEquals(paid, range.get(1));
        assertEquals(0, range.get(2).orders());
        assertEquals(paid, repo.daily(day));
    }
}
