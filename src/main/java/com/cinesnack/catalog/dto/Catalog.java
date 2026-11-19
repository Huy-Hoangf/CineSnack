package com.cinesnack.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class Catalog {

    private Catalog() {}

    public record Movie(
        @JsonProperty("id") long maSo,
        @JsonProperty("name") String ten,
        @JsonProperty("genre") String theLoai,
        @JsonProperty("description") String moTa,
        @JsonProperty("actors") String dienVien,
        @JsonProperty("duration") int soPhut,
        @JsonProperty("ageRating") String doTuoi,
        @JsonProperty("posterUrl") String poster,
        @JsonProperty("status") String trangThai,
        @JsonProperty("director") String daoDien,
        @JsonProperty("trailerUrl") String trailerUrl
    ) {
        public Movie(long maSo, String ten, String theLoai, String moTa, String dienVien,
            int soPhut, String doTuoi, String poster, String trangThai) {
            this(maSo, ten, theLoai, moTa, dienVien, soPhut, doTuoi, poster, trangThai, null, null);
        }
    }

    public record Show(
        @JsonProperty("id") long maSo,
        @JsonProperty("movieId") long maPhim,
        @JsonProperty("movieName") String tenPhim,
        @JsonProperty("roomId") long maPhong,
        @JsonProperty("roomName") String tenPhong,
        @JsonProperty("startsAt") OffsetDateTime batDau,
        @JsonProperty("endsAt") OffsetDateTime ketThuc,
        @JsonProperty("price") BigDecimal gia,
        @JsonProperty("vipSurcharge") BigDecimal phuThuVip,
        @JsonProperty("status") String trangThai,
        @JsonProperty("pricingMode") String pricingMode
    ) {
        public Show(long maSo, long maPhim, String tenPhim, long maPhong, String tenPhong,
            OffsetDateTime batDau, OffsetDateTime ketThuc, BigDecimal gia, BigDecimal phuThuVip, String trangThai) {
            this(maSo,maPhim,tenPhim,maPhong,tenPhong,batDau,ketThuc,gia,phuThuVip,trangThai,"MANUAL");
        }
        public Show(
            long maSo,
            long maPhim,
            String tenPhim,
            long maPhong,
            String tenPhong,
            OffsetDateTime batDau,
            OffsetDateTime ketThuc,
            BigDecimal gia,
            String trangThai
        ) {
            this(
                maSo,
                maPhim,
                tenPhim,
                maPhong,
                tenPhong,
                batDau,
                ketThuc,
                gia,
                BigDecimal.ZERO,
                trangThai
            );
        }
    }

    public record Seat(
        @JsonProperty("id") long maSo,
        @JsonProperty("code") String ma,
        @JsonProperty("type") String loai,
        @JsonProperty("available") boolean conTrong,
        @JsonProperty("price") BigDecimal gia
    ) {}

    public record Food(
        @JsonProperty("id") long maSo,
        @JsonProperty("name") String ten,
        @JsonProperty("price") BigDecimal gia
    ) {}

    public record Snacks(
        @JsonProperty("products") List<Food> sanPham,
        @JsonProperty("combos") List<Food> combo
    ) {}
}
