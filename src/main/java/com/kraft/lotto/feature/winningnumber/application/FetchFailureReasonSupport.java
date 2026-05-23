package com.kraft.lotto.feature.winningnumber.application;

final class FetchFailureReasonSupport {

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
        return "reason=" + ex.metricReason() + "; " + message;
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
        if (lower.contains("missing_field") || lower.contains("field missing")) {
            return "missing_field";
        }
        if (lower.contains("validation:") || lower.contains("round mismatch")
                || lower.contains("field is not integral") || lower.contains("field out of")) {
            return "validation";
        }
        if (lower.contains("transform:") || lower.contains("response transform failed")) {
            return "transform";
        }
        if (lower.contains("unexpected_return_value") || lower.contains("unexpected returnvalue")) {
            return "unexpected_return_value";
        }
        if (lower.contains("response parse failed") || lower.contains("parse failed")) {
            return "json_parse";
        }
        if (lower.contains("response is not json")) {
            return "non_json";
        }
        if (lower.contains("html response for expected round") || lower.contains("html_upstream_blocked")) {
            return "html_upstream_blocked";
        }
        if (lower.contains("timeout")) {
            return "timeout";
        }
        if (lower.contains("http error")) {
            return "http_error";
        }
        if (lower.contains("network")) {
            return "network";
        }
        if (lower.contains("circuit")) {
            return "circuit_open";
        }
        return "other";
    }
}
