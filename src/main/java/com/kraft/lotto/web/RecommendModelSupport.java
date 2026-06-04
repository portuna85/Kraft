package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendFilter;
import com.kraft.lotto.feature.recommend.application.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class RecommendModelSupport {

    private final RecommendService recommendService;

    void addRecommendModel(int count, Model model) {
        addRecommendModel(count, RecommendFilter.NONE, model);
    }

    void addRecommendModel(int count, RecommendFilter filter, Model model) {
        var recommendation = recommendService.recommend(count, filter);
        model.addAttribute("count", count);
        model.addAttribute("combinations", recommendation.combinations());
        model.addAttribute("rules", recommendService.rules());
        model.addAttribute("maxCount", PublicQueryParams.MAX_COUNT);
        model.addAttribute("filterOddCount", filter.oddCount());
        model.addAttribute("filterSumMin", filter.sumMin());
        model.addAttribute("filterSumMax", filter.sumMax());
        model.addAttribute("disabledRules", filter.disabledRules());
    }
}
