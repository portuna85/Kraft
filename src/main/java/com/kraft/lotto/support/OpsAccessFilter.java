package com.kraft.lotto.support;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
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

    private final KraftSecurityProperties securityProperties;
    private final List<IpRange> allowRules;
    private final MeterRegistry meterRegistry;

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
        this.meterRegistry = meterRegistry;
        this.allowRules = buildAllowRules(securityProperties.getOps().getAllowedIps());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/ops/") || path.startsWith("/admin/ops"));
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
        if (!isAllowed(clientIp)) {
            log.warn("Blocked ops access from ip={} path={}",
                    LogSanitizer.sanitizeLogValue(clientIp),
                    LogSanitizer.maskSensitivePath(request.getRequestURI()));
            response.sendError(HttpStatus.FORBIDDEN.value(), "Ops endpoint is not accessible from this IP.");
            return;
        }

        String requiredToken = securityProperties.getOps().getRequiredToken();
        if (!requiredToken.isBlank()) {
            String headerToken = request.getHeader(TOKEN_HEADER);
            if (headerToken == null || headerToken.isBlank() || !tokensMatch(requiredToken, headerToken.trim())) {
                log.warn("Blocked ops access due to missing/invalid header token ip={} path={}",
                        LogSanitizer.sanitizeLogValue(clientIp),
                        LogSanitizer.maskSensitivePath(request.getRequestURI()));
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid X-Ops-Token header.");
                return;
            }
        }

        log.info("Ops access granted ip={} method={} path={}",
                LogSanitizer.sanitizeLogValue(clientIp),
                request.getMethod(),
                LogSanitizer.maskSensitivePath(request.getRequestURI()));
        filterChain.doFilter(request, response);
    }

    private static boolean tokensMatch(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private boolean isAllowed(String clientIp) {
        try {
            InetAddress remote = InetAddress.getByName(clientIp);
            byte[] candidate = remote.getAddress();
            for (IpRange allowRule : allowRules) {
                if (allowRule.matches(candidate)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private List<IpRange> buildAllowRules(List<String> rawRules) {
        List<IpRange> rules = new ArrayList<>();
        for (String rawRule : rawRules) {
            String trimmed = rawRule == null ? "" : rawRule.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                rules.add(IpRange.parse(trimmed));
            } catch (Exception ex) {
                log.warn("Ignoring invalid ops allowlist entry: {}", LogSanitizer.sanitizeLogValue(trimmed));
                countAllowlistInvalid("ops");
            }
        }
        return rules;
    }

    private void countAllowlistInvalid(String filter) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("kraft.security.allowlist.invalid.entry", "filter", filter).increment();
    }

}
