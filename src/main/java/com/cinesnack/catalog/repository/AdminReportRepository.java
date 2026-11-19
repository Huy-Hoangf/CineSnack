package com.cinesnack.catalog.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminReportRepository {

    private final JdbcTemplate db;

    public AdminReportRepository(JdbcTemplate db) {
        this.db = db;
    }

    public record Daily(
        LocalDate date,
        long orders,
        long tickets,
        long snacks,
        BigDecimal ticketRevenue,
        BigDecimal snackRevenue,
        BigDecimal totalRevenue
    ) {}

    public java.util.List<Daily> range(LocalDate from, LocalDate to) {
        var rows = db.query(
            """
            WITH paid AS (
              SELECT d.don_hang_id, CAST(t.paid_at AS date) report_date
              FROM DON_HANG d JOIN (
                SELECT don_hang_id, MIN(thanh_toan_luc) paid_at FROM THANH_TOAN
                WHERE trang_thai='THANH_CONG' GROUP BY don_hang_id
              ) t ON t.don_hang_id=d.don_hang_id
              WHERE d.trang_thai='DA_THANH_TOAN' AND t.paid_at>=? AND t.paid_at<?
            )
            SELECT p.report_date, COUNT(*) orders, SUM(COALESCE(v.quantity,0)) tickets,
              SUM(COALESCE(s.quantity,0)) snacks,
              SUM(COALESCE(v.revenue,0)) ticket_revenue,
              SUM(COALESCE(s.revenue,0)) snack_revenue
            FROM paid p LEFT JOIN (
              SELECT don_hang_id, COUNT(*) quantity, SUM(don_gia) revenue FROM VE
              WHERE trang_thai IN ('DA_THANH_TOAN','DA_SOAT') GROUP BY don_hang_id
            ) v ON v.don_hang_id=p.don_hang_id LEFT JOIN (
              SELECT don_hang_id, SUM(CAST(so_luong AS bigint)) quantity, SUM(so_luong*don_gia) revenue
              FROM CHI_TIET_HOA_DON_BONG_NUOC
              WHERE trang_thai_phuc_vu IN ('CHO_NHAN','DA_NHAN') GROUP BY don_hang_id
            ) s ON s.don_hang_id=p.don_hang_id
            GROUP BY p.report_date ORDER BY p.report_date
            """,
            (r, i) -> {
                BigDecimal ticket = r.getBigDecimal("ticket_revenue");
                BigDecimal snack = r.getBigDecimal("snack_revenue");
                return new Daily(r.getDate("report_date").toLocalDate(), r.getLong("orders"),
                    r.getLong("tickets"), r.getLong("snacks"), ticket, snack, ticket.add(snack));
            }, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        var byDate = new java.util.HashMap<LocalDate, Daily>();
        rows.forEach(row -> byDate.put(row.date(), row));
        return from.datesUntil(to.plusDays(1)).map(date -> byDate.getOrDefault(date,
            new Daily(date, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))).toList();
    }

    public Daily daily(LocalDate date) {
        return range(date, date).get(0);
    }
}
