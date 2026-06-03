package com.kraft.lotto.feature.winningnumber.web.dto;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record DataChangeLogDto(
        int round,
        LottoFetchStatus status,
        String statusLabel,
        String statusCssClass,
        String fetchedAtFormatted
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static DataChangeLogDto of(int round, LottoFetchStatus status, LocalDateTime fetchedAt) {
        return new DataChangeLogDto(
                round,
                status,
                toLabel(status),
                toCssClass(status),
                fetchedAt != null ? fetchedAt.format(FMT) : ""
        );
    }

    private static String toLabel(LottoFetchStatus status) {
        return switch (status) {
            case SUCCESS   -> "수집 성공";
            case FAILED    -> "수집 실패";
            case SKIPPED   -> "스킵";
            case NOT_DRAWN -> "미추첨";
        };
    }

    private static String toCssClass(LottoFetchStatus status) {
        return switch (status) {
            case SUCCESS   -> "changelog-success";
            case FAILED    -> "changelog-failed";
            case SKIPPED   -> "changelog-skipped";
            case NOT_DRAWN -> "changelog-not-drawn";
        };
    }
}
