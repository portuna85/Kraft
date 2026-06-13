package com.kraft.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:1}") String baselineVersion) {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();
    }
}
