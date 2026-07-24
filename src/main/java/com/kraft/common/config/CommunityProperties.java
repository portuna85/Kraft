package com.kraft.common.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kraft.community")
public record CommunityProperties(
        @Min(1) int writeRateLimitPerMinute
) {
}
