package com.kraft.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraft.ops")
public record OpsProperties(
        String token
) {
}
