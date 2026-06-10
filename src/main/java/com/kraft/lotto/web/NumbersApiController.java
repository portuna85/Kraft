package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendFilter;
import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@RequestMapping("/api/v1/numbers")
@Validated
@RequiredArgsConstructor
@Tag(name = "Numbers", description = "번호 추천")
public class NumbersApiController {

    private final RecommendService recommendService;

    @PostMapping("/recommend")
    @Operation(summary = "번호 조합 추천. 필터(홀수 개수·합산 범위·비활성 규칙) 선택 적용")
    public ResponseEntity<ApiResponse<RecommendResponse>> recommend(
            @RequestBody @Valid RecommendRequest request) {
        RecommendFilter filter = RecommendFilter.of(
                request.oddCount(), request.sumMin(), request.sumMax(), request.disabledRules());
        return ResponseEntity.ok(ApiResponse.success(
                recommendService.recommend(request.count(), filter)));
    }

    @GetMapping("/recommend/rules")
    @Operation(summary = "추천 시 적용되는 제외 규칙 목록 조회")
    public ResponseEntity<ApiResponse<List<RuleDto>>> rules() {
        return ResponseEntity.ok(ApiResponse.success(recommendService.rules()));
    }

    public record RecommendRequest(
            @Min(1) @Max(10) int count,
            Integer oddCount,
            Integer sumMin,
            Integer sumMax,
            List<String> disabledRules
    ) {
        public RecommendRequest {
            disabledRules = disabledRules == null ? List.of() : List.copyOf(disabledRules);
        }
    }
}
