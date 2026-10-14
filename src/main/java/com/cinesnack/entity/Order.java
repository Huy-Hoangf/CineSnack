package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
        @JsonProperty("id") long maSo,
        @JsonProperty("userId") Long maNguoiDung,
        @JsonProperty("code") String ma,
        @JsonProperty("email") String thuDienTu,
        @JsonProperty("total") BigDecimal tongTien,
        @JsonProperty("status") String trangThai,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss'+07:00'") @JsonProperty("expiresAt") LocalDateTime hetHanLuc,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss'+07:00'") @JsonProperty("createdAt") LocalDateTime taoLuc) {}
