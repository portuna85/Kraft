package com.kraft.lotto.feature.winningnumber.application;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLException;

final class FetchFailureReasonSupport {
    private static final String[][] REASON_RULES = {
            {"missing_field", "missing_field", "field missing"},
            {"validation", "validation:", "round mismatch", "field is not integral", "field out of"},
            {"transform", "transform:", "response transform failed"},
            {"unexpected_return_value", "unexpected_return_value", "unexpected returnvalue"},
            {"json_parse", "response parse failed", "parse failed"},
            {"non_json", "response is not json"},
            {"html_upstream_blocked", "html response for expected round", "html_upstream_blocked"},
            {"timeout", "timeout"},
            {"http_error", "http error"},
            {"network", "network"},
            {"circuit_open", "circuit"}
    };

    private FetchFailureReasonSupport() {
    }

    static String normalizeFailureMessage(String message) {
        String safe = message == null ? "" : message;
        if (safe.startsWith("reason=")) {
            return safe;
        }
        return "reason=" + classifyFailureReason(safe) + "; " + safe;
    }

    static String normalizeFailureMessage(LottoApiClientException ex) {
        if (ex == null) {
            return normalizeFailureMessage((String) null);
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return "reason=" + classifyFailureReason(ex) + "; " + message;
    }

    static String extractReason(String message) {
        if (message == null || message.isBlank()) {
            return "other";
        }
        String trimmed = message.trim();
        if (!trimmed.startsWith("reason=")) {
            return "other";
        }
        int semicolon = trimmed.indexOf(';');
        String token = semicolon < 0 ? trimmed.substring("reason=".length()) : trimmed.substring("reason=".length(), semicolon);
        String reason = token.trim().toLowerCase();
        return reason.isBlank() ? "other" : reason;
    }

    static String stripReasonPrefix(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String trimmed = message.trim();
        if (!trimmed.startsWith("reason=")) {
            return trimmed;
        }
        int semicolon = trimmed.indexOf(';');
        if (semicolon < 0 || semicolon + 1 >= trimmed.length()) {
            return "";
        }
        return trimmed.substring(semicolon + 1).trim();
    }

    private static String classifyFailureReason(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        for (String[] rule : REASON_RULES) {
            String reason = rule[0];
            for (int i = 1; i < rule.length; i++) {
                if (lower.contains(rule[i])) {
                    return reason;
                }
            }
        }
        return "other";
    }

    private static String classifyFailureReason(LottoApiClientException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            return LottoApiClientException.FailureReason.TIMEOUT.metricName();
        }
        if (cause instanceof ConnectException) {
            return LottoApiClientException.FailureReason.NETWORK.metricName();
        }
        if (cause instanceof SSLException) {
            return LottoApiClientException.FailureReason.OTHER.metricName();
        }
        String metricReason = ex.metricReason();
        if (metricReason != null && !metricReason.isBlank()
                && !LottoApiClientException.FailureReason.OTHER.metricName().equals(metricReason)) {
            return metricReason;
        }
        return classifyFailureReason(ex.getMessage());
    }
}
