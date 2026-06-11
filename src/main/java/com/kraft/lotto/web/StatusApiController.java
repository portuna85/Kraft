package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.support.ApiResponse;
import com.kraft.lotto.web.dto.ServiceStatusDto;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatusApiController {

    private static final DateTimeFormatter BUILD_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WinningNumberRepository winningNumberRepository;
    private final LottoFetchLogQueryService fetchLogQueryService;
    private final Clock clock;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ServiceStatusDto>> status() {
        LocalDate today = LocalDate.now(clock);
        int expectedRound = LottoDrawSchedule.expectedRound(today);

        var latest = winningNumberRepository.findTopByOrderByRoundDesc().orElse(null);
        Integer latestRound = latest != null ? latest.getRound() : null;
        String latestDrawDate = latest != null
                ? latest.getDrawDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : null;

        int roundsBehind = (latestRound != null) ? Math.max(0, expectedRound - latestRound) : 0;
        boolean upToDate = latestRound != null && latestRound >= expectedRound;

        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        String appVersion = bp != null ? bp.getVersion() : null;
        String buildTime = (bp != null && bp.getTime() != null)
                ? bp.getTime().atZone(ZoneId.of("Asia/Seoul")).format(BUILD_TIME_FMT)
                : null;

        var dto = new ServiceStatusDto(
                latestRound,
                latestDrawDate,
                expectedRound,
                upToDate,
                roundsBehind,
                fetchLogQueryService.recentCollectionLogs(20),
                appVersion,
                buildTime
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(ApiResponse.success(dto));
    }
}
