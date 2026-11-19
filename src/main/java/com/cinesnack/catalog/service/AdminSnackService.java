package com.cinesnack.catalog.service;

import com.cinesnack.catalog.dto.AdminSnack;
import com.cinesnack.catalog.repository.AdminSnackRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSnackService {

    private final AdminSnackRepository repo;

    public AdminSnackService(AdminSnackRepository repo) {
        this.repo = repo;
    }

    public List<AdminSnack> list() {
        return repo.list();
    }

    public AdminSnack create(
        boolean product,
        String name,
        String productType,
        BigDecimal price,
        String status
    ) {
        return repo.create(
            product,
            cleanName(name),
            cleanType(product, productType),
            price,
            status
        );
    }

    public AdminSnack update(
        boolean product,
        long id,
        String name,
        String productType,
        BigDecimal price,
        String status
    ) {
        AdminSnack changed = repo.update(
            product,
            id,
            name == null ? null : cleanName(name),
            productType == null ? null : cleanType(product, productType),
            price,
            status
        );
        if (changed == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy món"
            );
        }
        return changed;
    }

    @Transactional
    public void delete(boolean product, long id) {
        if (
            repo.usedInOrder(product, id) || (product && repo.usedInCombo(id))
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Món đã có trong đơn hoặc combo; hãy ngừng bán."
            );
        }
        try {
            if (!product) repo.deleteComboItems(id);
            if (repo.delete(product, id) != 1) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy món"
                );
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Món đã được sử dụng; hãy ngừng bán."
            );
        }
    }

    private String cleanName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tên món phải có từ 1 đến 100 ký tự."
            );
        }
        return name.trim();
    }

    private String cleanType(boolean product, String type) {
        if (!product) return null;
        if (
            !"BAP".equals(type) && !"NUOC".equals(type) && !"KHAC".equals(type)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Loại món không hợp lệ."
            );
        }
        return type;
    }
}
