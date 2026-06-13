package com.kraft.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.stereotype.Component;

import java.util.Set;

// Registers Flyway beans as database initializers so that Spring Boot's
// JpaDependsOnDatabaseInitializationDetector ensures JPA waits for Flyway
// to complete migrations before Hibernate schema validation runs.
@Component
public class FlywayDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

    @Override
    protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
        return Set.of(Flyway.class);
    }
}
