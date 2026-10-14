package com.cinesnack.controller;
import com.cinesnack.dto.Catalog;
import com.cinesnack.service.CatalogService;
import com.cinesnack.service.OrderExpiryService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @Validated
public class CatalogController {
    private final CatalogService danhMuc;
    private final OrderExpiryService hetHan;
    public CatalogController(CatalogService danhMuc,OrderExpiryService hetHan) { this.danhMuc=danhMuc; this.hetHan=hetHan; }
    @GetMapping("/api/movies")
    public List<Catalog.Movie> layPhim(@RequestParam(name="q",defaultValue="") @Size(max=100) String tu,
        @RequestParam(name="status",defaultValue="") @Pattern(regexp="|DANG_CHIEU|SAP_CHIEU") String trangThai,
        @RequestParam(name="page",defaultValue="0") @Min(0) @Max(1000000) int trang,
        @RequestParam(name="size",defaultValue="20") @Min(1) @Max(100) int soLuong) {
        return danhMuc.layPhim(tu,trangThai,trang,soLuong);
    }
    @GetMapping("/api/movies/{id}")
    public Catalog.Movie layPhimTheoMa(@PathVariable("id") @Positive long maPhim) {
        return danhMuc.layPhimTheoMa(maPhim);
    }
    @GetMapping("/api/showtimes")
    public List<Catalog.Show> laySuat(@RequestParam(name="movieId",required=false) @Positive Long maPhim) { return danhMuc.laySuat(maPhim); }
    @GetMapping("/api/showtimes/{id}/seats")
    public List<Catalog.Seat> layGhe(@PathVariable("id") @Positive long maSuat) { hetHan.xuLyHetHan(); return danhMuc.layGhe(maSuat); }
    @GetMapping("/api/snacks")
    public Catalog.Snacks layBapNuoc() { return danhMuc.layBapNuoc(); }
}
