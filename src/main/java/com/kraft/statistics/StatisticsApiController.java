package com.kraft.statistics;

import com.kraft.common.error.ApiException;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
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
    public PatternStatsResponse patterns() {
        return statisticsService.getPatternStats();
    }

    @GetMapping("/companion")
    public CompanionStatsResponse companion(@RequestParam(required = false) Integer ball) {
        if (ball == null) {
            return statisticsService.getCompanionStats();
        }
        if (ball < 1 || ball > 45) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BALL",
                    "ball 허용 범위: 1~45");
        }
        return statisticsService.getCompanionStatsByBall(ball);
    }

    @PostMapping("/analysis")
    public AnalysisResponse analysis(@Valid @RequestBody AnalysisRequest request) {
        List<Integer> numbers = request.numbers();
        validateNumbers(numbers);
        return statisticsService.analyze(numbers);
    }

    private void validateNumbers(List<Integer> numbers) {
        Set<Integer> seen = new HashSet<>();
        for (Integer n : numbers) {
            if (n == null || n < 1 || n > 45) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NUMBER",
                        "로또 번호는 1~45 사이여야 합니다: " + n);
            }
            if (!seen.add(n)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_NUMBER",
                        "중복 번호가 있습니다: " + n);
            }
        }
    }
}
