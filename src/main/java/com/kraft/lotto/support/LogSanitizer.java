package com.kraft.lotto.support;

public final class LogSanitizer {

    private static final String SENSITIVE_KEYS = "token|secret|key|password|passwd|pwd|authorization|access_token";

    private LogSanitizer() {
    }

    public static String sanitizeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\r', '_').replace('\n', '_');
    }

    public static String maskSensitiveQuery(String queryString) {
        String sanitized = sanitizeLogValue(queryString);
        if (sanitized.isBlank()) {
            return "";
        }
        return sanitized.replaceAll("(?i)(" + SENSITIVE_KEYS + ")=([^&\\s]*)", "$1=***");
    }

    public static String maskSensitivePath(String path) {
        String sanitized = sanitizeLogValue(path);
        if (sanitized.isBlank()) {
            return "";
        }
        return sanitized.replaceAll("(?i)(/(" + SENSITIVE_KEYS + ")/)[^/?#\\s]*", "$1***");
    }
}
