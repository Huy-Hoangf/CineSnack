package com.cinesnack.service;

import com.cinesnack.dto.Catalog;
import com.cinesnack.repository.CatalogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogMovieServiceTest {
    @Test void traPhimSapChieuTheoIdKhongPhuThuocTrangLoc() {
        var kho = mock(CatalogRepository.class);
        var phim = new Catalog.Movie(123, "Phim sắp chiếu", "Hoạt hình", 90, "P", null, "SAP_CHIEU");
        when(kho.timPhim(123)).thenReturn(phim);
        assertEquals(phim, new CatalogService(kho, Clock.systemUTC()).layPhimTheoMa(123));
    }
    @Test void phimKhongConHienThiTra404() {
        var kho = mock(CatalogRepository.class);
        var loi = assertThrows(ResponseStatusException.class,
            () -> new CatalogService(kho, Clock.systemUTC()).layPhimTheoMa(999));
        assertEquals(404, loi.getStatusCode().value());
    }
}
