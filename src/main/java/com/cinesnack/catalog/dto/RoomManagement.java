package com.cinesnack.catalog.dto;

import jakarta.validation.constraints.*;

public final class RoomManagement {

    private RoomManagement() {}

    public record Room(
        long id,
        String name,
        String type,
        String status,
        int seatCount
    ) {}

    public record Seat(long id, String code, String type, String status) {}

    public record RoomRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 30) String type,
        @NotNull @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String status
    ) {}

    public record SetupRequest(
        @Min(1) @Max(26) int rows,
        @Min(1) @Max(30) int columns,
        @NotNull @Pattern(regexp = "THUONG|VIP") String type
    ) {}

    public record SeatRequest(
        @NotNull @Pattern(regexp = "[A-Z](?:[1-9]|[12][0-9]|30)") String code,
        @NotNull @Pattern(regexp = "THUONG|VIP") String type,
        @NotNull @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String status
    ) {}
}
