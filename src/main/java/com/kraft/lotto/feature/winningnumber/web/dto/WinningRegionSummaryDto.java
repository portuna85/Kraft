package com.kraft.lotto.feature.winningnumber.web.dto;

public record WinningRegionSummaryDto(
        int round,
        int grade,
        String sido,
        String sigungu,
        long count
) {}
