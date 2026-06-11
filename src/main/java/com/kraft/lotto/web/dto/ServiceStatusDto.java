package com.kraft.lotto.web.dto;

import com.kraft.lotto.feature.winningnumber.web.dto.DataChangeLogDto;
import java.util.List;

public record ServiceStatusDto(
        Integer latestRound,
        String latestDrawDate,
        int expectedRound,
        boolean upToDate,
        int roundsBehind,
        List<DataChangeLogDto> recentLogs,
        String appVersion,
        String buildTime
) {}
