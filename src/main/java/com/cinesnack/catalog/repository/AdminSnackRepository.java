package com.cinesnack.catalog.repository;

import com.cinesnack.catalog.dto.AdminSnack;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AdminSnackRepository {

    private final JdbcTemplate db;

    public AdminSnackRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<AdminSnack> list() {
        var products = db.query(
            "SELECT san_pham_id,ten_san_pham,gia_ban,trang_thai,loai_san_pham FROM SAN_PHAM ORDER BY san_pham_id",
            (row, index) -> map(row, true)
        );
        products.addAll(
            db.query(
                "SELECT combo_id,ten_combo,gia_ban,trang_thai FROM COMBO ORDER BY combo_id",
                (row, index) -> map(row, false)
            )
        );
        return products;
    }

    public AdminSnack create(
        boolean product,
        String name,
        String productType,
        BigDecimal price,
        String status
    ) {
        String sql = product
            ? "INSERT INTO SAN_PHAM(ten_san_pham,loai_san_pham,gia_ban,trang_thai) VALUES(?,?,?,?)"
            : "INSERT INTO COMBO(ten_combo,gia_ban,trang_thai) VALUES(?,?,?)";
        String idColumn = product ? "san_pham_id" : "combo_id";
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] {
                idColumn,
            });
            statement.setString(1, name);
            if (product) {
                statement.setString(2, productType);
                statement.setBigDecimal(3, price);
                statement.setString(4, status);
            } else {
                statement.setBigDecimal(2, price);
                statement.setString(3, status);
            }
            return statement;
        }, key);
        return find(product, key.getKey().longValue());
    }

    public AdminSnack update(
        boolean product,
        long id,
        String name,
        String productType,
        BigDecimal price,
        String status
    ) {
        int count = product
            ? db.update(
                  "UPDATE SAN_PHAM SET ten_san_pham=COALESCE(?,ten_san_pham),loai_san_pham=COALESCE(?,loai_san_pham),gia_ban=?,trang_thai=? WHERE san_pham_id=?",
                  name,
                  productType,
                  price,
                  status,
                  id
              )
            : db.update(
                  "UPDATE COMBO SET ten_combo=COALESCE(?,ten_combo),gia_ban=?,trang_thai=? WHERE combo_id=?",
                  name,
                  price,
                  status,
                  id
              );
        return count == 1 ? find(product, id) : null;
    }

    public boolean usedInOrder(boolean product, long id) {
        String column = product ? "san_pham_id" : "combo_id";
        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM CHI_TIET_HOA_DON_BONG_NUOC WHERE " +
                column +
                "=?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    public boolean usedInCombo(long productId) {
        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM COMBO_SAN_PHAM WHERE san_pham_id=?",
            Integer.class,
            productId
        );
        return count != null && count > 0;
    }

    public void deleteComboItems(long comboId) {
        db.update("DELETE FROM COMBO_SAN_PHAM WHERE combo_id=?", comboId);
    }

    public int delete(boolean product, long id) {
        return product
            ? db.update("DELETE FROM SAN_PHAM WHERE san_pham_id=?", id)
            : db.update("DELETE FROM COMBO WHERE combo_id=?", id);
    }

    private AdminSnack find(boolean product, long id) {
        String sql = product
            ? "SELECT san_pham_id,ten_san_pham,gia_ban,trang_thai,loai_san_pham FROM SAN_PHAM WHERE san_pham_id=?"
            : "SELECT combo_id,ten_combo,gia_ban,trang_thai FROM COMBO WHERE combo_id=?";
        return db.queryForObject(sql, (row, index) -> map(row, product), id);
    }

    private AdminSnack map(ResultSet row, boolean product) throws SQLException {
        return new AdminSnack(
            row.getLong(1),
            product ? "PRODUCT" : "COMBO",
            row.getString(2),
            row.getBigDecimal(3),
            row.getString(4),
            product ? row.getString(5) : null
        );
    }
}
