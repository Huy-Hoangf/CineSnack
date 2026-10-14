package com.cinesnack.support;

import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class Sql {
    private Sql() {}

    public static long themVaLayMa(JdbcTemplate truyVan, String cauLenh, Object... thamSo) {
        Long maSo = truyVan.queryForObject(cauLenh, Long.class, thamSo);
        if (maSo == null) throw new IllegalStateException("Thiếu ID sau khi tạo dữ liệu");
        return maSo;
    }

    public static LocalDateTime docThoiGian(ResultSet dong, String tenCot) throws SQLException {
        var giaTri = dong.getTimestamp(tenCot);
        return giaTri == null ? null : giaTri.toLocalDateTime();
    }
}
