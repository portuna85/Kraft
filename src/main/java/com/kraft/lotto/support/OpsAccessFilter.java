package com.kraft.lotto.support;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class OpsAccessFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OpsAccessFilter.class);
    private static final String TOKEN_HEADER = "X-Ops-Token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final KraftSecurityProperties securityProperties;
    private final IpAllowlist allowlist;

    @Autowired
    public OpsAccessFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider,
                           ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new), meterRegistryProvider.getIfAvailable());
    }

    OpsAccessFilter(KraftSecurityProperties securityProperties) {
        this(securityProperties, null);
    }

    OpsAccessFilter(KraftSecurityProperties securityProperties, MeterRegistry meterRegistry) {
        this.securityProperties = securityProperties;
        this.allowlist = IpAllowlist.parse(securityProperties.getOps().getAllowedIps(), "ops", log, meterRegistry);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(isOpsApiPath(path) || isOpsAdminPath(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityProperties.getOps().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIpResolver.resolve(request, securityProperties.getTrustedProxies());
        if (!allowlist.isAllowed(clientIp)) {
            log.warn("Blocked ops access from ip={} path={}",
                    LogSanitizer.sanitizeLogValue(clientIp),
                    LogSanitizer.maskSensitivePath(request.getRequestURI()));
            response.sendError(HttpStatus.FORBIDDEN.value(), "Ops endpoint is not accessible from this IP.");
            return;
        }

        String requiredToken = securityProperties.getOps().getRequiredToken();
        if (!requiredToken.isBlank() && !hasValidOpsToken(requiredToken, request)) {
            log.warn("Blocked ops access due to missing/invalid token ip={} path={}",
                    LogSanitizer.sanitizeLogValue(clientIp),
                    LogSanitizer.maskSensitivePath(request.getRequestURI()));
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid ops token.");
            return;
        }

        log.info("Ops access granted ip={} method={} path={}",
                LogSanitizer.sanitizeLogValue(clientIp),
                request.getMethod(),
                LogSanitizer.maskSensitivePath(request.getRequestURI()));
        filterChain.doFilter(request, response);
    }

    private static boolean hasValidOpsToken(String expectedToken, HttpServletRequest request) {
        String providedToken = resolveProvidedToken(request);
        return providedToken != null && tokensMatch(expectedToken, providedToken);
    }

    private static String resolveProvidedToken(HttpServletRequest request) {
        String explicitHeaderToken = trimToNull(request.getHeader(TOKEN_HEADER));
        if (explicitHeaderToken != null) {
            return explicitHeaderToken;
        }

        String authorization = trimToNull(request.getHeader(AUTHORIZATION_HEADER));
        if (authorization == null) {
            return null;
        }
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return trimToNull(authorization.substring(BEARER_PREFIX.length()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isOpsApiPath(String path) {
        return "/ops".equals(path) || path.startsWith("/ops/");
    }

    private static boolean isOpsAdminPath(String path) {
        return "/admin/ops".equals(path) || path.startsWith("/admin/ops/");
    }

    private static boolean tokensMatch(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

}
