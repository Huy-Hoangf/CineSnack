package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(
    @JsonProperty("id") long maSo,
    @JsonProperty("orderId") long maDonHang,
    @JsonProperty("transactionCode") String maGiaoDich,
    @JsonProperty("method") String phuongThuc,
    @JsonProperty("amount") BigDecimal soTien,
    @JsonProperty("status") String trangThai,
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'+07:00'")
    @JsonProperty("paidAt")
    LocalDateTime thanhToanLuc
) {}
