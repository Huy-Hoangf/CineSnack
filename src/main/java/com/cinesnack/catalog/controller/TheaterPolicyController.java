package com.cinesnack.catalog.controller;

import com.cinesnack.catalog.dto.TheaterPolicy;
import com.cinesnack.catalog.service.TheaterPolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin")
public class TheaterPolicyController {
    private final TheaterPolicyService service;
    public TheaterPolicyController(TheaterPolicyService service) { this.service=service; }
    @GetMapping("/rooms/{id}/pricing")
    public TheaterPolicy.RoomPricing pricing(@PathVariable @Positive long id, Principal user) { return service.pricing(id,user.getName()); }
    @PutMapping("/rooms/{id}/pricing")
    public TheaterPolicy.RoomPricing savePricing(@PathVariable @Positive long id, @Valid @RequestBody TheaterPolicy.RoomPricing request, Principal user) {
        return service.savePricing(id,request,user.getName());
    }
    @GetMapping("/theater-policy")
    public TheaterPolicy.Settings policy(Principal user) { return service.policy(user.getName()); }
    @PutMapping("/theater-policy")
    public TheaterPolicy.Settings savePolicy(@Valid @RequestBody TheaterPolicy.Settings request, Principal user) { return service.savePolicy(request,user.getName()); }
}
