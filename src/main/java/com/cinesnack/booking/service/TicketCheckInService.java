package com.cinesnack.booking.service;

import com.cinesnack.account.service.UserService;
import com.cinesnack.booking.entity.Ticket;
import com.cinesnack.catalog.service.TheaterPolicyService;
import com.cinesnack.booking.repository.TicketCheckInRepository;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketCheckInService {

    private final TicketCheckInRepository repo;
    private final UserService users;
    private final Clock clock;
    private final TheaterPolicyService policy;

    public TicketCheckInService(
        TicketCheckInRepository repo,
        UserService users,
        Clock clock,
        TheaterPolicyService policy
    ) {
        this.repo = repo;
        this.users = users;
        this.clock = clock;
        this.policy = policy;
    }

    public record Result(
        Ticket ticket,
        OffsetDateTime checkedAt,
        long staffId
    ) {}

    public record PrintResult(
        Ticket ticket,
        OffsetDateTime printedAt,
        long staffId
    ) {}

    @Transactional
    public PrintResult issue(String code, String email) {
        var staff = users.layTaiKhoanHoatDong(email);
        if (
            !"NHAN_VIEN".equals(staff.vaiTro())
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ nhân viên được in vé."
        );
        LocalDateTime now = LocalDateTime.now(
            clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        ).withNano(0);
        if (
            repo.claimPrint(code.trim(), staff.maSo(), now) != 1
        ) throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Vé chưa thanh toán, đã in, đã soát hoặc không hợp lệ."
        );
        return new PrintResult(
            repo.ticket(code.trim()),
            now.atOffset(ZoneOffset.ofHours(7)),
            staff.maSo()
        );
    }

    @Transactional
    public java.util.List<PrintResult> issueBatch(java.util.List<String> codes, String email) {
        if (codes.isEmpty() || codes.size() > 100 ||
            codes.stream().map(c -> c.trim().toUpperCase(java.util.Locale.ROOT)).distinct().count() != codes.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách vé không hợp lệ hoặc bị trùng");
        }
        return codes.stream().map(code -> issue(code, email)).toList();
    }

    @Transactional
    public Result checkIn(String code, String email) {
        var staff = users.layTaiKhoanHoatDong(email);
        if (!staff.laNhanVien()) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ nhân viên hoặc quản trị được soát vé."
        );
        LocalDateTime now = LocalDateTime.now(
            clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        ).withNano(0);
        if (
            repo.checkIn(code.trim(), staff.maSo(), now, policy.currentPolicy().lateCheckInMinutes()) != 1
        ) throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Vé không hợp lệ, đã soát, chưa thanh toán hoặc ngoài giờ soát theo chính sách rạp (mở từ 30 phút trước giờ chiếu, luôn trước khi kết thúc)."
        );
        return new Result(
            repo.ticket(code.trim()),
            now.atOffset(ZoneOffset.ofHours(7)),
            staff.maSo()
        );
    }
}
