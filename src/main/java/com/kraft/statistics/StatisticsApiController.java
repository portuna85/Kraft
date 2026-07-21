package com.kraft.statistics;

import com.kraft.common.error.ApiException;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatisticsApiController {

    private static final Set<Integer> ALLOWED_LIMITS = Set.of(100, 200, 500);

    private final WinningStatisticsCacheService statisticsService;

    public StatisticsApiController(WinningStatisticsCacheService statisticsService) {
        this.statisticsService = statisticsService;
    }

    // PublicApiCacheControlFilter가 이론상 이 응답에도 동일한 Cache-Control을 다시 설정하지만,
    // 실제 운영 환경에서 컨트롤러가 일반 객체를 반환하면 필터가 헤더를 못 붙이는 사례가
    // 확인되어(2026-07-21 배포 실패로 발견) ResponseEntity로 명시적으로 설정한다.
    @GetMapping("/frequency")
    public ResponseEntity<FrequencyStatsResponse> frequency(@RequestParam(required = false) Integer limit) {
        FrequencyStatsResponse body;
        if (limit == null) {
            body = statisticsService.getFrequencyStats();
        } else {
            if (!ALLOWED_LIMITS.contains(limit)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LIMIT",
                        "limit 허용값: 100, 200, 500");
            }
            body = statisticsService.getFrequencyStatsByLimit(limit);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(body);
    }

    @GetMapping("/patterns")
    public ResponseEntity<PatternStatsResponse> patterns() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(statisticsService.getPatternStats());
    }

    @GetMapping("/companion")
    public ResponseEntity<CompanionStatsResponse> companion(@RequestParam(required = false) Integer ball) {
        CompanionStatsResponse body;
        if (ball == null) {
            body = statisticsService.getCompanionStats();
        } else {
            if (ball < 1 || ball > 45) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BALL",
                        "ball 허용 범위: 1~45");
            }
            body = statisticsService.getCompanionStatsByBall(ball);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(body);
    }

    @PostMapping("/analysis")
    public AnalysisResponse analysis(@Valid @RequestBody AnalysisRequest request) {
        return statisticsService.analyze(request.numbers());
    }
}
