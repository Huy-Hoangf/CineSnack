package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Invoice(
    @JsonProperty("id") long maSo,
    @JsonProperty("orderId") long maDonHang,
    @JsonProperty("code") String ma,
    @JsonProperty("type") String loai,
    @JsonProperty("email") String thuDienTu,
    @JsonProperty("total") BigDecimal tongTien,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'+07:00'")
    @JsonProperty("issuedAt")
    LocalDateTime phatHanhLuc,
    @JsonProperty("lines") List<Line> dsDong
) {
    public record Line(
        @JsonProperty("type") String loai,
        @JsonProperty("description") String moTa,
        @JsonProperty("quantity") int soLuong,
        @JsonProperty("unitPrice") BigDecimal donGia
    ) {}
}
