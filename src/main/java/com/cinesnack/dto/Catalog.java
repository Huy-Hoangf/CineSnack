package com.cinesnack.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class Catalog {
    private Catalog() {}
    public record Movie(@JsonProperty("id") long maSo, @JsonProperty("name") String ten,
        @JsonProperty("genre") String theLoai, @JsonProperty("duration") int soPhut,
        @JsonProperty("ageRating") String doTuoi, @JsonProperty("posterUrl") String poster,
        @JsonProperty("status") String trangThai) {}
    public record Show(@JsonProperty("id") long maSo, @JsonProperty("movieId") long maPhim,
        @JsonProperty("movieName") String tenPhim, @JsonProperty("roomId") long maPhong,
        @JsonProperty("roomName") String tenPhong, @JsonProperty("startsAt") OffsetDateTime batDau,
        @JsonProperty("endsAt") OffsetDateTime ketThuc, @JsonProperty("price") BigDecimal gia,
        @JsonProperty("status") String trangThai) {}
    public record Seat(@JsonProperty("id") long maSo, @JsonProperty("code") String ma,
        @JsonProperty("type") String loai, @JsonProperty("available") boolean conTrong,
        @JsonProperty("price") BigDecimal gia) {}
    public record Food(@JsonProperty("id") long maSo, @JsonProperty("name") String ten,
        @JsonProperty("price") BigDecimal gia) {}
    public record Snacks(@JsonProperty("products") List<Food> sanPham, @JsonProperty("combos") List<Food> combo) {}
}
