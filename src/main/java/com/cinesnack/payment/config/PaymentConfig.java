package com.cinesnack.payment.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class PaymentConfig {

    @Bean
    public Clock dongHoRap() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Bean
    public TransactionTemplate mauGiaoDich(PlatformTransactionManager boQuanLyGiaoDich) {
        return new TransactionTemplate(boQuanLyGiaoDich);
    }
}
