package com.cinesnack.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    public CsrfToken maCsrf(CsrfToken maCsrf) { return maCsrf; }
}
