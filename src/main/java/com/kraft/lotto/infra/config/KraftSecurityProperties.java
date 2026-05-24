package com.kraft.lotto.infra.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kraft.security")
@Validated
public class KraftSecurityProperties {

    @Valid
    private final Headers headers = new Headers();

    @Valid
    private final Actuator actuator = new Actuator();

    @Valid
    private final RateLimit rateLimit = new RateLimit();

    @Valid
    private final Ops ops = new Ops();

    private List<String> trustedProxies = new ArrayList<>(List.of());

    public List<String> getTrustedProxies() {
        return List.copyOf(trustedProxies);
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null ? new ArrayList<>() : new ArrayList<>(trustedProxies);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring @ConfigurationProperties exposes nested mutable groups by design")
    public Headers getHeaders() {
        return headers;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring @ConfigurationProperties exposes nested mutable groups by design")
    public Actuator getActuator() {
        return actuator;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring @ConfigurationProperties exposes nested mutable groups by design")
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Spring @ConfigurationProperties exposes nested mutable groups by design")
    public Ops getOps() {
        return ops;
    }

    public static class Headers {
        private boolean enabled = true;

        @NotBlank
        private String contentSecurityPolicy =
                "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; "
                        + "style-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:; "
                        + "font-src 'self' https://cdn.jsdelivr.net; connect-src 'self'; object-src 'none'; "
                        + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

        @NotBlank
        private String xFrameOptions = "DENY";

        @NotBlank
        private String referrerPolicy = "strict-origin-when-cross-origin";

        @NotBlank
        private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

        private boolean hstsEnabled = false;
        private long hstsMaxAgeSeconds = 31536000L;
        private boolean hstsIncludeSubDomains = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getXFrameOptions() {
            return xFrameOptions;
        }

        public void setXFrameOptions(String xFrameOptions) {
            this.xFrameOptions = xFrameOptions;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public long getHstsMaxAgeSeconds() {
            return hstsMaxAgeSeconds;
        }

        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }

        public boolean isHstsIncludeSubDomains() {
            return hstsIncludeSubDomains;
        }

        public void setHstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
            this.hstsIncludeSubDomains = hstsIncludeSubDomains;
        }
    }

    public static class Actuator {
        private boolean enabled = true;
        private boolean trustForwardedFor = false;
        private List<String> allowedIps = new ArrayList<>(List.of(
                "127.0.0.1",
                "::1",
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16"
        ));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrustForwardedFor() {
            return trustForwardedFor;
        }

        public void setTrustForwardedFor(boolean trustForwardedFor) {
            this.trustForwardedFor = trustForwardedFor;
        }

        public List<String> getAllowedIps() {
            return List.copyOf(allowedIps);
        }

        public void setAllowedIps(List<String> allowedIps) {
            this.allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private boolean trustForwardedFor = false;

        @Min(1)
        private int maxRequests = 120;

        @Min(1)
        private int windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrustForwardedFor() {
            return trustForwardedFor;
        }

        public void setTrustForwardedFor(boolean trustForwardedFor) {
            this.trustForwardedFor = trustForwardedFor;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public static class Ops {
        private boolean enabled = true;
        private boolean trustForwardedFor = false;
        private String requiredToken = "";
        private List<String> allowedIps = new ArrayList<>(List.of(
                "127.0.0.1",
                "::1",
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16"
        ));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrustForwardedFor() {
            return trustForwardedFor;
        }

        public void setTrustForwardedFor(boolean trustForwardedFor) {
            this.trustForwardedFor = trustForwardedFor;
        }

        public String getRequiredToken() {
            return requiredToken;
        }

        public void setRequiredToken(String requiredToken) {
            this.requiredToken = requiredToken == null ? "" : requiredToken.trim();
        }

        public List<String> getAllowedIps() {
            return List.copyOf(allowedIps);
        }

        public void setAllowedIps(List<String> allowedIps) {
            this.allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
        }
    }
}
