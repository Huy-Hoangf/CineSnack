package com.cinesnack.catalog.controller;

import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.repository.AdminReportRepository;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportRepository repo;
    private final UserService users;

    public AdminReportController(
        AdminReportRepository repo,
        UserService users
    ) {
        this.repo = repo;
        this.users = users;
    }

    @GetMapping("/daily")
    public AdminReportRepository.Daily daily(
        @RequestParam @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate date,
        Principal principal
    ) {
        if (
            !"QUAN_TRI".equals(
                users.layTaiKhoanHoatDong(principal.getName()).vaiTro()
            )
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ quản trị được xem báo cáo"
        );
        return repo.daily(date);
    }
    @GetMapping("/range")
    public java.util.List<AdminReportRepository.Daily> range(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Principal principal
    ) {
        if (!"QUAN_TRI".equals(users.layTaiKhoanHoatDong(principal.getName()).vaiTro())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị được xem báo cáo");
        }
        if (from.isAfter(to) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365
            || from.isBefore(LocalDate.of(1, 1, 1)) || to.isAfter(LocalDate.of(9998, 12, 31))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn khoảng từ 1 đến 366 ngày hợp lệ");
        }
        return repo.range(from, to);
    }
}
