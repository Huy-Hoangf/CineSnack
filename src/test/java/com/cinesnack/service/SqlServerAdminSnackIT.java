package com.cinesnack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cinesnack.catalog.repository.AdminSnackRepository;
import com.cinesnack.catalog.service.AdminSnackService;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@SpringJUnitConfig(SqlServerAdminSnackIT.Config.class)
@EnabledIfEnvironmentVariable(named = "CINESNACK_TEST_DB_URL", matches = ".+")
@Transactional
class SqlServerAdminSnackIT {

    @Configuration
    @EnableTransactionManagement
    @Import({ AdminSnackRepository.class, AdminSnackService.class })
    static class Config {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                System.getenv("CINESNACK_TEST_DB_URL")
            );
        }

        @Bean
        JdbcTemplate jdbc(DataSource source) {
            return new JdbcTemplate(source);
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
    }

    @Autowired
    JdbcTemplate db;

    @Autowired
    AdminSnackService snacks;

    @Test
    void createsAndDeletesOnRealSqlServerThenRollsBack() {
        assertEquals(
            "CineSnack",
            db.queryForObject("SELECT DB_NAME()", String.class)
        );
        var product = snacks.create(
            true,
            "Kiểm thử bắp",
            "BAP",
            new BigDecimal("35000"),
            "HOAT_DONG"
        );
        var combo = snacks.create(
            false,
            "Kiểm thử combo",
            null,
            new BigDecimal("59000"),
            "HOAT_DONG"
        );
        assertTrue(product.id() > 0);
        assertTrue(combo.id() > 0);
        assertEquals(
            "Kiểm thử bắp",
            snacks
                .update(
                    true,
                    product.id(),
                    null,
                    null,
                    new BigDecimal("36000"),
                    "HOAT_DONG"
                )
                .name()
        );
        snacks.delete(false, combo.id());
        snacks.delete(true, product.id());
    }
}
