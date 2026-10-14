package com.cinesnack.service;

import com.cinesnack.dto.Catalog;
import com.cinesnack.repository.CatalogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {

    private final CatalogRepository kho;
    private final Clock dongHo;

    public CatalogService(CatalogRepository kho, Clock dongHo) {
        this.kho = kho;
        this.dongHo = dongHo;
    }

    public List<Catalog.Movie> layPhim(String tu, String trangThai, int trang, int soLuong) {
        return kho.layPhim(tu, trangThai, trang, soLuong);
    }

    public Catalog.Movie layPhimTheoMa(long maPhim) {
        var phim = kho.timPhim(maPhim);
        if (phim == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy phim"
        );
        return phim;
    }

    public List<Catalog.Show> laySuat(Long maPhim) {
        return kho.laySuat(maPhim, LocalDateTime.now(dongHo));
    }

    public List<Catalog.Seat> layGhe(long maSuat) {
        var ds = kho.layGhe(maSuat, LocalDateTime.now(dongHo));
        if (ds.isEmpty()) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy suất chiếu hoặc ghế"
        );
        return ds;
    }

    public Catalog.Snacks layBapNuoc() {
        return kho.layBapNuoc();
    }
}
