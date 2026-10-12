
package com.cinesnack.entity;

public class Movie {

    private Long phimId;
    private String tenPhim;
    private String theLoai;
    private Integer thoiLuongPhut;
    private String phanLoaiDoTuoi;
    private String posterUrl;
    private String trangThai;

    public Movie() {
    }

    public Movie(Long phimId, String tenPhim, String theLoai,
                 Integer thoiLuongPhut, String phanLoaiDoTuoi,
                 String posterUrl, String trangThai) {
        this.phimId = phimId;
        this.tenPhim = tenPhim;
        this.theLoai = theLoai;
        this.thoiLuongPhut = thoiLuongPhut;
        this.phanLoaiDoTuoi = phanLoaiDoTuoi;
        this.posterUrl = posterUrl;
        this.trangThai = trangThai;
    }

    public Long getPhimId() {
        return phimId;
    }

    public void setPhimId(Long phimId) {
        this.phimId = phimId;
    }

    public String getTenPhim() {
        return tenPhim;
    }

    public void setTenPhim(String tenPhim) {
        this.tenPhim = tenPhim;
    }

    public String getTheLoai() {
        return theLoai;
    }

    public void setTheLoai(String theLoai) {
        this.theLoai = theLoai;
    }

    public Integer getThoiLuongPhut() {
        return thoiLuongPhut;
    }

    public void setThoiLuongPhut(Integer thoiLuongPhut) {
        this.thoiLuongPhut = thoiLuongPhut;
    }

    public String getPhanLoaiDoTuoi() {
        return phanLoaiDoTuoi;
    }

    public void setPhanLoaiDoTuoi(String phanLoaiDoTuoi) {
        this.phanLoaiDoTuoi = phanLoaiDoTuoi;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
