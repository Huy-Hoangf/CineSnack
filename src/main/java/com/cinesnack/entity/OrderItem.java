package com.cinesnack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderItem(
    @JsonProperty("id") long maSo,
    @JsonProperty("productId") Long maSanPham,
    @JsonProperty("comboId") Long maCombo,
    @JsonProperty("name") String ten,
    @JsonProperty("quantity") int soLuong,
    @JsonProperty("unitPrice") BigDecimal donGia,
    @JsonProperty("status") String trangThai
) {}
