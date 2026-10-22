package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Ticket(
    @JsonProperty("id") long maSo,
    @JsonProperty("showTimeId") long maSuatChieu,
    @JsonProperty("seatId") long maGhe,
    @JsonProperty("seatCode") String nhanGhe,
    @JsonProperty("code") String ma,
    @JsonProperty("unitPrice") BigDecimal donGia,
    @JsonProperty("status") String trangThai,
    @JsonProperty("movieName") String tenPhim,
    @JsonProperty("roomName") String tenPhong,
    @JsonProperty("startsAt") OffsetDateTime batDau
) {}
