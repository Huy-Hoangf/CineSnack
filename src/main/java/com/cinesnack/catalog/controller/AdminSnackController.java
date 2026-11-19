package com.cinesnack.catalog.controller;

import com.cinesnack.account.service.UserService;
import com.cinesnack.catalog.dto.AdminSnack;
import com.cinesnack.catalog.service.AdminSnackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/admin/snacks")
public class AdminSnackController {

    private final AdminSnackService snacks;
    private final UserService users;

    public AdminSnackController(AdminSnackService snacks, UserService users) {
        this.snacks = snacks;
        this.users = users;
    }

    public record Change(
        @Size(max = 100) String name,
        @Pattern(regexp = "BAP|NUOC|KHAC") String productType,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 0)
        BigDecimal price,
        @NotBlank @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String status
    ) {}

    public record Create(
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "BAP|NUOC|KHAC") String productType,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 0)
        BigDecimal price,
        @NotBlank @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String status
    ) {}

    private void admin(Principal principal) {
        if (
            !"QUAN_TRI".equals(
                users.layTaiKhoanHoatDong(principal.getName()).vaiTro()
            )
        ) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Chỉ quản trị được sửa danh mục bắp nước"
        );
    }

    @GetMapping
    public List<AdminSnack> list(Principal principal) {
        admin(principal);
        return snacks.list();
    }

    @PostMapping("/{kind}")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSnack create(
        @PathVariable @Pattern(regexp = "products|combos") String kind,
        @Valid @RequestBody Create request,
        Principal principal
    ) {
        admin(principal);
        return snacks.create(
            "products".equals(kind),
            request.name(),
            request.productType(),
            request.price(),
            request.status()
        );
    }

    @PutMapping("/{kind}/{id}")
    public AdminSnack update(
        @PathVariable @Pattern(regexp = "products|combos") String kind,
        @PathVariable @Positive long id,
        @Valid @RequestBody Change change,
        Principal principal
    ) {
        admin(principal);
        return snacks.update(
            "products".equals(kind),
            id,
            change.name(),
            change.productType(),
            change.price(),
            change.status()
        );
    }

    @DeleteMapping("/{kind}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable @Pattern(regexp = "products|combos") String kind,
        @PathVariable @Positive long id,
        Principal principal
    ) {
        admin(principal);
        snacks.delete("products".equals(kind), id);
    }
}
