package com.kraft.lotto.support;

import io.micrometer.core.instrument.MeterRegistry;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class IpAllowlist {

    private static final Logger log = LoggerFactory.getLogger(IpAllowlist.class);

    private final List<IpRange> rules;

    static IpAllowlist parse(List<String> rawCidrs, String filterName, Logger log, MeterRegistry meterRegistry) {
        List<IpRange> rules = new ArrayList<>();
        for (String rawRule : rawCidrs) {
            String trimmed = rawRule == null ? "" : rawRule.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                rules.add(IpRange.parse(trimmed));
            } catch (Exception ex) {
                log.warn("Ignoring invalid {} allowlist entry: {}", filterName, LogSanitizer.sanitizeLogValue(trimmed));
                meterRegistry.counter("kraft.security.allowlist.invalid.entry", "filter", filterName).increment();
            }
        }
        return new IpAllowlist(rules);
    }

    private IpAllowlist(List<IpRange> rules) {
        this.rules = List.copyOf(rules);
    }

    boolean isAllowed(String clientIp) {
        try {
            InetAddress remote = InetAddress.getByName(clientIp);
            byte[] candidate = remote.getAddress();
            for (IpRange rule : rules) {
                if (rule.matches(candidate)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.warn("IP 주소 해석 실패: {}", LogSanitizer.sanitizeLogValue(clientIp));
            return false;
        }
    }
}
