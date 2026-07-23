package com.kraft.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kraft.community")
public record CommunityProperties(
        boolean enabled
) {
}
