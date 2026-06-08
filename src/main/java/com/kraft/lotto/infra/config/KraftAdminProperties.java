package com.kraft.lotto.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.admin")
@Validated
public record KraftAdminProperties(
        boolean enabled,
        @NotBlank String adminDomain,
        String adminPasswordHash,
        List<@Valid AdminUser> users
) {
    public KraftAdminProperties {
        users = users != null ? List.copyOf(users) : null;
    }

    public record AdminUser(
            @NotBlank String username,
            @NotBlank String passwordHash,
            @NotEmpty List<String> roles
    ) {
        public AdminUser {
            roles = roles != null ? List.copyOf(roles) : List.of();
        }
    }

    public boolean hasConfiguredUsers() {
        return users != null && !users.isEmpty();
    }
}
