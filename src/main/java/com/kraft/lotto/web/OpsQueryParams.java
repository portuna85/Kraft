package com.kraft.lotto.web;

final class OpsQueryParams {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 2000;
    private static final int MAX_REASON_LENGTH = 100;
    private static final int MAX_TOKEN_LENGTH = 200;

    private OpsQueryParams() {
    }

    static int normalizeLimit(int limit) {
        return Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
    }

    static Integer normalizeDrwNo(Integer drwNo) {
        if (drwNo == null) {
            return null;
        }
        return Math.max(1, drwNo);
    }

    static Range normalizeRange(Integer drwNoFrom, Integer drwNoTo) {
        Integer from = normalizeDrwNo(drwNoFrom);
        Integer to = normalizeDrwNo(drwNoTo);
        if (from != null && to != null && from > to) {
            int temp = from;
            from = to;
            to = temp;
        }
        return new Range(from, to);
    }

    static String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        trimmed = trimmed.replaceAll("[^a-z0-9_:\\-.]", "");
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_REASON_LENGTH) {
            return trimmed.substring(0, MAX_REASON_LENGTH);
        }
        return trimmed;
    }

    static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        String trimmed = token.trim();
        if (trimmed.length() > MAX_TOKEN_LENGTH) {
            return trimmed.substring(0, MAX_TOKEN_LENGTH);
        }
        return trimmed;
    }

    record Range(Integer from, Integer to) {
    }
}
