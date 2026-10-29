package com.cinesnack.account.controller;

import com.cinesnack.account.dto.RegisterRequest;
import com.cinesnack.account.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegistrationController {
    private final RegistrationService dangKy;

    public RegistrationController(RegistrationService dangKy) {
        this.dangKy = dangKy;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void dangKy(@Valid @RequestBody RegisterRequest yeuCau) {
        dangKy.dangKy(yeuCau);
    }
}
