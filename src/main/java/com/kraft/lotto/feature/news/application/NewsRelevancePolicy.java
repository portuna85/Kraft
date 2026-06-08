package com.kraft.lotto.feature.news.application;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public final class NewsRelevancePolicy {

    // 로또 관련 핵심 신호 (긍정)
    private static final List<SignalEntry> ALLOW_SIGNALS = List.of(
        new SignalEntry("동행복권", 5),
        new SignalEntry("당첨번호", 5),
        new SignalEntry("로또1등", 4),
        new SignalEntry("로또 1등", 4),
        new SignalEntry("1등 당첨", 4),
        new SignalEntry("판매점", 4),
        new SignalEntry("추첨", 3),
        new SignalEntry("회차", 2)
    );

    // 무관 도메인 신호 (부정)
    private static final List<SignalEntry> BLOCK_SIGNALS = List.of(
        new SignalEntry("청약", 5),
        new SignalEntry("분양", 5),
        new SignalEntry("부동산", 4),
        new SignalEntry("전세", 4),
        new SignalEntry("아파트", 4),
        new SignalEntry("선거", 3),
        new SignalEntry("정치", 3),
        new SignalEntry("주식", 2),
        new SignalEntry("비트코인", 2),
        new SignalEntry("암호화폐", 2),
        new SignalEntry("코인", 2)
    );

    private static final int APPROVE_THRESHOLD = 4;

    public NewsDecision decide(String title, String description, String source, String link) {
        String target = buildTarget(title, description, source, link);

        int score = 0;
        for (SignalEntry entry : ALLOW_SIGNALS) {
            if (target.contains(entry.keyword())) {
                score += entry.weight();
            }
        }
        for (SignalEntry entry : BLOCK_SIGNALS) {
            if (target.contains(entry.keyword())) {
                score -= entry.weight();
            }
        }

        if (score < 0) {
            return new NewsDecision(NewsDecision.Type.REJECT, score);
        }
        if (score >= APPROVE_THRESHOLD) {
            return new NewsDecision(NewsDecision.Type.APPROVE, score);
        }
        return new NewsDecision(NewsDecision.Type.REVIEW, score);
    }

    private static String buildTarget(String title, String description, String source, String link) {
        return String.join(" ",
            nullToEmpty(title),
            nullToEmpty(description),
            nullToEmpty(source),
            nullToEmpty(link)
        ).toLowerCase(Locale.KOREAN);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private record SignalEntry(String keyword, int weight) {}
}
