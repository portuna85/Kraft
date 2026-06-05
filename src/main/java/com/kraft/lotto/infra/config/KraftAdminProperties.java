package com.kraft.lotto.infra.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.admin")
@Validated
public record KraftAdminProperties(
        boolean enabled,
        @NotBlank String adminDomain,
        String adminPassword
) {
}
