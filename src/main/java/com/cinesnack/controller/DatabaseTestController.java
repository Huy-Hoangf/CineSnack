
package com.cinesnack.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/test-db")
    public String testDatabase() {
        Integer result = jdbcTemplate.queryForObject(
            "SELECT 1", Integer.class
        );

        return result != null && result == 1
                ? "Ket noi SQL Server thanh cong!"
                : "Ket noi that bai!";
    }
}
