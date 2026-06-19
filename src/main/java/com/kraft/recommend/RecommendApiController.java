package com.kraft.recommend;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public CombinationCheckResponse check(@RequestParam List<Integer> numbers) {
        return new CombinationCheckResponse(lottoRecommendationService.isHistoricalFirstPrizeCombination(numbers));
    }
}
