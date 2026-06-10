package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.support.ApiResponse;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1/stats")
@Validated
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "통계 분석")
public class StatisticsApiController {

    // 통계 캐시 TTL은 Caffeine 설정(10분)과 맞춤
    private static final CacheControl STATS_CACHE =
            CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic();

    private final WinningStatisticsService statisticsService;

    @GetMapping("/frequency")
    @Operation(summary = "번호별 출현 빈도 조회. period 파라미터 지정 시 최근 N회차 기준 집계")
    public ResponseEntity<ApiResponse<List<NumberFrequencyDto>>> frequency(
            @RequestParam(required = false) @Min(1) Integer period) {
        List<NumberFrequencyDto> result = period != null
                ? statisticsService.frequencyForPeriod(period)
                : statisticsService.frequency();
        return ResponseEntity.ok()
                .cacheControl(STATS_CACHE)
                .body(ApiResponse.success(result));
    }

    @GetMapping("/patterns")
    @Operation(summary = "홀짝·합산 범위 패턴 통계 조회")
    public ResponseEntity<ApiResponse<PatternStatDto>> patterns() {
        return ResponseEntity.ok()
                .cacheControl(STATS_CACHE)
                .body(ApiResponse.success(statisticsService.patternStats()));
    }

    @GetMapping("/companion")
    @Operation(summary = "특정 번호와 함께 출현한 번호 순위 조회")
    public ResponseEntity<ApiResponse<List<CompanionNumberDto>>> companion(
            @RequestParam @Min(1) @Max(45) int target) {
        return ResponseEntity.ok()
                .cacheControl(STATS_CACHE)
                .body(ApiResponse.success(statisticsService.companionNumbers(target)));
    }

    @PostMapping("/analysis")
    @Operation(summary = "번호 조합의 과거 당첨 이력 분석")
    public ResponseEntity<ApiResponse<CombinationPrizeHistoryDto>> analysis(
            @RequestBody @Valid AnalysisRequest request) {
        List<Integer> sorted = request.numbers().stream().sorted().toList();
        if (new HashSet<>(sorted).size() < 6) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER, "중복된 번호가 포함되어 있습니다.");
        }
        return ResponseEntity.ok(ApiResponse.success(statisticsService.combinationPrizeHistory(sorted)));
    }

    public record AnalysisRequest(
            @NotNull @Size(min = 6, max = 6, message = "번호는 정확히 6개여야 합니다.")
            List<@NotNull @Min(1) @Max(45) Integer> numbers
    ) {
        public AnalysisRequest {
            numbers = List.copyOf(numbers);
        }
    }
}
