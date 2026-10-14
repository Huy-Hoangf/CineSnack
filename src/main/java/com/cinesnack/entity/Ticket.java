package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Ticket(
        @JsonProperty("id") long maSo,
        @JsonProperty("showTimeId") long maSuatChieu,
        @JsonProperty("seatId") long maGhe,
        @JsonProperty("seatCode") String nhanGhe,
        @JsonProperty("code") String ma,
        @JsonProperty("unitPrice") BigDecimal donGia,
        @JsonProperty("status") String trangThai) {}
