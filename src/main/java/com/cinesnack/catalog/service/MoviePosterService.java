package com.cinesnack.catalog.service;

import com.cinesnack.catalog.dto.AdminCatalog;
import com.cinesnack.catalog.dto.Catalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MoviePosterService {
    private final AdminCatalogService catalog;
    private final PosterService posters;

    public MoviePosterService(AdminCatalogService catalog, PosterService posters) {
        this.catalog = catalog;
        this.posters = posters;
    }

    @Transactional
    public Catalog.Movie save(Long id, AdminCatalog.MovieRequest request, MultipartFile poster, String email) {
        catalog.kiemTraQuyenQuanTri(email);
        String posterUrl = request.posterUrl();
        if (poster != null) {
            posterUrl = posters.luu(poster);
            String uploaded = posterUrl;
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) posters.xoaAnhMoi(uploaded);
                    }
                });
            } catch (RuntimeException ex) {
                posters.xoaAnhMoi(uploaded);
                throw ex;
            }
        }
        var updated = new AdminCatalog.MovieRequest(request.name(), request.genre(), request.duration(), request.ageRating(), posterUrl, request.status());
        return catalog.saveMovie(id, updated, email);
    }
}
