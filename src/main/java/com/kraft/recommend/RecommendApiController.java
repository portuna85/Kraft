package com.kraft.recommend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/numbers")
public class RecommendApiController {

    private final LottoRecommendationService lottoRecommendationService;

    public RecommendApiController(LottoRecommendationService lottoRecommendationService) {
        this.lottoRecommendationService = lottoRecommendationService;
    }

    @PostMapping("/recommend")
    public RecommendNumbersResponse recommend(@Valid @RequestBody(required = false) RecommendNumbersRequest request) {
        return lottoRecommendationService.recommend(request);
    }

    @GetMapping("/check")
    public CombinationCheckResponse check(
            @RequestParam @Size(min = 6, max = 6, message = "번호는 정확히 6개여야 합니다.")
            List<@NotNull @Min(1) @Max(45) Integer> numbers) {
        return new CombinationCheckResponse(lottoRecommendationService.isHistoricalFirstPrizeCombination(numbers));
    }
}
