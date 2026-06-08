package com.kraft.lotto.feature.news.application;

public record NewsDecision(Type type, int score) {

    public enum Type { APPROVE, REVIEW, REJECT }

    public String rejectReason() {
        return type == Type.REJECT ? "score:" + score : null;
    }
}
