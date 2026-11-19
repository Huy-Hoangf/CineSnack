package com.cinesnack.booking.controller;

import com.cinesnack.booking.service.TicketCheckInService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/tickets")
public class TicketCheckInController {

    private final TicketCheckInService service;

    public TicketCheckInController(TicketCheckInService service) {
        this.service = service;
    }

    public record Request(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,30}") String code
    ) {}

    @PostMapping("/scan")
    public TicketCheckInService.Result checkIn(
        @Valid @RequestBody Request request,
        Principal user
    ) {
        return service.checkIn(request.code(), user.getName());
    }

    public record BatchRequest(
        @NotEmpty @Size(max = 100)
        java.util.List<@NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,30}") String> codes
    ) {}

    @PostMapping("/issue-batch")
    public java.util.List<TicketCheckInService.PrintResult> issueBatch(
        @Valid @RequestBody BatchRequest request, Principal user
    ) {
        return service.issueBatch(request.codes(), user.getName());
    }

    @PostMapping("/issue")
    public TicketCheckInService.PrintResult issue(
        @Valid @RequestBody Request request,
        Principal user
    ) {
        return service.issue(request.code(), user.getName());
    }
}
