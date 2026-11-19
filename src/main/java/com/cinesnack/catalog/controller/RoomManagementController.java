package com.cinesnack.catalog.controller;

import com.cinesnack.catalog.dto.RoomManagement.*;
import com.cinesnack.catalog.service.RoomManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/rooms")
public class RoomManagementController {

    private final RoomManagementService service;

    public RoomManagementController(RoomManagementService service) {
        this.service = service;
    }

    @GetMapping("/management")
    public List<Room> rooms(
        @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Principal user
    ) {
        return service.rooms(page, size, user.getName());
    }

    @GetMapping("/archived")
    public List<Room> archived(
        @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Principal user
    ) {
        return service.archivedRooms(page, size, user.getName());
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable @Positive long id, Principal user) {
        service.restore(id, user.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(
        @Valid @RequestBody RoomRequest request,
        Principal user
    ) {
        return Map.of("id", service.save(null, request, user.getName()));
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(
        @PathVariable @Positive long id,
        @Valid @RequestBody RoomRequest request,
        Principal user
    ) {
        return Map.of("id", service.save(id, request, user.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id, Principal user) {
        service.delete(id, user.getName());
    }

    @GetMapping("/{id}/seats")
    public List<Seat> seats(@PathVariable @Positive long id, Principal user) {
        return service.seats(id, user.getName());
    }

    @PostMapping("/{id}/seats/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Seat> setup(
        @PathVariable @Positive long id,
        @Valid @RequestBody SetupRequest request,
        Principal user
    ) {
        return service.setup(id, request, user.getName());
    }

    @PutMapping("/{room}/seats/{seat}")
    public Seat seat(
        @PathVariable @Positive long room,
        @PathVariable @Positive long seat,
        @Valid @RequestBody SeatRequest request,
        Principal user
    ) {
        return service.saveSeat(room, seat, request, user.getName());
    }
}
