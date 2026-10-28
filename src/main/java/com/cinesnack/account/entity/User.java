package com.cinesnack.account.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public record User(
    @JsonProperty("id") long maSo,
    @JsonProperty("email") String thuDienTu,
    @JsonProperty("passwordHash") String matKhauHash,
    @JsonProperty("role") String vaiTro,
    @JsonProperty("status") String trangThai
) {
    public boolean laNhanVien() {
        return "NHAN_VIEN".equals(vaiTro) || "QUAN_TRI".equals(vaiTro);
    }
}
