package com.kraft.lotto.support;

import jakarta.servlet.http.HttpServletRequest;

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
}
