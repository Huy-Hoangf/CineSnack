package com.cinesnack.account.controller;

import com.cinesnack.account.dto.StaffAccount;
import com.cinesnack.account.repository.AdminStaffRepository;
import com.cinesnack.account.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final AdminStaffRepository repo;
    private final UserService users;
    private final PasswordEncoder encoder;

    public AdminStaffController(
        AdminStaffRepository repo,
        UserService users,
        PasswordEncoder encoder
    ) {
        this.repo = repo;
        this.users = users;
        this.encoder = encoder;
    }

    public record Create(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull @Pattern(regexp = "NHAN_VIEN|QUAN_TRI") String role
    ) {}

    public record Status(
        @NotNull @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String status
    ) {}

    private void admin(Principal principal) {
        if (
            !"QUAN_TRI".equals(
                users.layTaiKhoanHoatDong(principal.getName()).vaiTro()
            )
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ quản trị được quản lý nhân viên"
        );
    }

    @GetMapping
    public List<StaffAccount> list(Principal principal) {
        admin(principal);
        return repo.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffAccount create(
        @Valid @RequestBody Create request,
        Principal principal
    ) {
        admin(principal);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Mật khẩu không được vượt quá 72 byte"
            );
        }
        String email = request
            .email()
            .trim()
            .toLowerCase(java.util.Locale.ROOT);
        try {
            repo.create(
                request.name().trim(),
                email,
                encoder.encode(request.password()),
                request.role()
            );
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email đã được sử dụng"
            );
        }
        return repo.byEmail(email);
    }

    @PutMapping("/{id}/status")
    public StaffAccount status(
        @PathVariable @Positive long id,
        @Valid @RequestBody Status request,
        Principal principal
    ) {
        admin(principal);
        if (
            repo.status(id, request.status()) != 1
        ) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy nhân viên"
        );
        return repo.byId(id);
    }
}
