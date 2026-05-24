package com.kraft.lotto.support;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.List;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] candidates = forwardedFor.split(",");
                for (String candidate : candidates) {
                    String trimmed = candidate == null ? "" : candidate.trim();
                    if (!trimmed.isBlank()) {
                        return trimmed;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }

    public static String resolve(HttpServletRequest request, List<String> trustedProxyCidrs) {
        if (!trustedProxyCidrs.isEmpty()) {
            String remoteAddr = request.getRemoteAddr();
            if (isTrustedProxy(remoteAddr, trustedProxyCidrs)) {
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isBlank()) {
                    for (String candidate : forwardedFor.split(",")) {
                        String trimmed = candidate.trim();
                        if (!trimmed.isBlank()) {
                            return trimmed;
                        }
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isTrustedProxy(String remoteAddr, List<String> trustedProxyCidrs) {
        try {
            InetAddress remote = InetAddress.getByName(remoteAddr);
            byte[] candidate = remote.getAddress();
            for (String cidr : trustedProxyCidrs) {
                try {
                    if (IpRange.parse(cidr).matches(candidate)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
