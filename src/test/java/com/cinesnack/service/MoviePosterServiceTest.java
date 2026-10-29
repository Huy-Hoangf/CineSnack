package com.cinesnack.service;

import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.service.AdminCatalogService;
import com.cinesnack.catalog.service.MoviePosterService;
import com.cinesnack.catalog.service.PosterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoviePosterServiceTest {
    private final AdminCatalogService catalog = mock(AdminCatalogService.class);
    private final PosterService posters = mock(PosterService.class);
    private final MoviePosterService service = new MoviePosterService(catalog, posters);
    private final AdminCatalog.MovieRequest movie = new AdminCatalog.MovieRequest("Phim", "Hành động", 120, "T13", null, "SAP_CHIEU");
    private final MockMultipartFile file = new MockMultipartFile("poster", "p.png", "image/png", new byte[]{1});

    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void removesNewPosterOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        when(posters.luu(file)).thenReturn("/posters/test.png");
        doThrow(new IllegalStateException()).when(catalog).saveMovie(isNull(), any(), eq("admin"));
        assertThrows(IllegalStateException.class, () -> service.save(null, movie, file, "admin"));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(posters).xoaAnhMoi("/posters/test.png");
    }

    @Test void keepsNewPosterOnCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(posters.luu(file)).thenReturn("/posters/test.png");
        service.save(null, movie, file, "admin");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(posters, never()).xoaAnhMoi(anyString());
        verify(catalog).saveMovie(isNull(), argThat(r -> "/posters/test.png".equals(r.posterUrl())), eq("admin"));
    }
}
