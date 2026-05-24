package com.kraft.lotto.support;

import com.kraft.lotto.infra.config.KraftSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ActuatorAccessFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ActuatorAccessFilter.class);

    private final KraftSecurityProperties securityProperties;
    private final List<IpRange> allowRules;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ActuatorAccessFilter(ObjectProvider<KraftSecurityProperties> securityPropertiesProvider,
                                ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this(securityPropertiesProvider.getIfAvailable(KraftSecurityProperties::new), meterRegistryProvider.getIfAvailable());
    }

    ActuatorAccessFilter(KraftSecurityProperties securityProperties) {
        this(securityProperties, null);
    }

    ActuatorAccessFilter(KraftSecurityProperties securityProperties, MeterRegistry meterRegistry) {
        this.securityProperties = securityProperties;
        this.meterRegistry = meterRegistry;
        this.allowRules = buildAllowRules(securityProperties.getActuator().getAllowedIps());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityProperties.getActuator().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIpResolver.resolve(request, securityProperties.getTrustedProxies());
        if (isAllowed(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Blocked actuator access from ip={} path={}",
                LogSanitizer.sanitizeLogValue(clientIp),
                LogSanitizer.maskSensitivePath(request.getRequestURI()));
        response.sendError(HttpStatus.FORBIDDEN.value(), "Actuator endpoint is not accessible from this IP.");
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
                log.warn("Ignoring invalid actuator allowlist entry: {}", LogSanitizer.sanitizeLogValue(trimmed));
                countAllowlistInvalid("actuator");
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
