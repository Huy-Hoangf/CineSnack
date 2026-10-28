package com.cinesnack.service;

import com.cinesnack.catalog.service.CatalogService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cinesnack.catalog.dto.Catalog;
import com.cinesnack.catalog.repository.CatalogRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CatalogMovieServiceTest {

    @Test
    void traPhimSapChieuTheoIdKhongPhuThuocTrangLoc() {
        var kho = mock(CatalogRepository.class);
        var phim = new Catalog.Movie(
            123,
            "Phim sắp chiếu",
            "Hoạt hình",
            90,
            "P",
            null,
            "SAP_CHIEU"
        );
        when(kho.timPhim(123)).thenReturn(phim);
        assertEquals(phim, new CatalogService(kho, Clock.systemUTC()).layPhimTheoMa(123));
    }

    @Test
    void phimKhongConHienThiTra404() {
        var kho = mock(CatalogRepository.class);
        var loi = assertThrows(ResponseStatusException.class, () ->
            new CatalogService(kho, Clock.systemUTC()).layPhimTheoMa(999)
        );
        assertEquals(404, loi.getStatusCode().value());
    }
}
