package com.cinesnack.catalog.service;

import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.TheaterPolicy;
import com.cinesnack.catalog.repository.TheaterPolicyRepository;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TheaterPolicyService {
    private final TheaterPolicyRepository repo;
    private final UserService users;
    public TheaterPolicyService(TheaterPolicyRepository repo, UserService users) { this.repo=repo; this.users=users; }
    @Transactional(readOnly=true)
    public TheaterPolicy.RoomPricing pricing(long roomId, String email) {
        requireAdmin(email);
        if (!repo.roomExists(roomId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy phòng.");
        return requirePricing(roomId);
    }
    @Transactional
    public TheaterPolicy.RoomPricing savePricing(long roomId, TheaterPolicy.RoomPricing pricing, String email) {
        requireAdmin(email);
        if (!repo.lockRoom(roomId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy phòng.");
        repo.savePricing(roomId,pricing);
        return pricing;
    }
    @Transactional(readOnly=true)
    public TheaterPolicy.Settings policy(String email) { requireAdmin(email); return currentPolicy(); }
    @Transactional
    public TheaterPolicy.Settings savePolicy(TheaterPolicy.Settings policy, String email) {
        requireAdmin(email); repo.savePolicy(policy); return policy;
    }
    @Transactional(readOnly=true)
    public TheaterPolicy.Settings currentPolicy() { return repo.policy(); }
    // Called only when an administrator explicitly saves a POLICY show; the show snapshots these values.
    public TheaterPolicy.ShowPrices pricesForShow(long roomId, OffsetDateTime startsAt) {
        var p = requirePricing(roomId);
        var day = startsAt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return new TheaterPolicy.ShowPrices(weekend ? p.weekendRegular() : p.weekdayRegular(), weekend ? p.weekendVip() : p.weekdayVip());
    }
    private TheaterPolicy.RoomPricing requirePricing(long roomId) {
        var p=repo.pricing(roomId);
        if(p==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phòng chưa có bảng giá. Hãy cấu hình trước khi lấy giá theo chính sách.");
        return p;
    }
    private void requireAdmin(String email) {
        if(!"QUAN_TRI".equals(users.layTaiKhoanHoatDong(email).vaiTro()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ quản trị được cấu hình giá và chính sách rạp.");
    }
}
