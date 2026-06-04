package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Validated
@Controller
@RequiredArgsConstructor
public class RecommendController {

    private static final String RECOMMEND_FRAGMENT = "fragments/recommend-card :: recommend-card";

    private final RecommendModelSupport recommendModelSupport;

    @GetMapping("/recommend")
    public RedirectView recommendRedirect() {
        RedirectView rv = new RedirectView("/");
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return rv;
    }

    @GetMapping("/fragments/recommend")
    public String recommend(
            @RequestParam(defaultValue = "5") @Min(PublicQueryParams.MIN_COUNT) @Max(PublicQueryParams.MAX_COUNT) int count,
            @RequestParam(required = false) Integer oddCount,
            @RequestParam(required = false) Integer sumMin,
            @RequestParam(required = false) Integer sumMax,
            @RequestParam(required = false) List<String> disabledRules,
            Model model
    ) {
        RecommendFilter filter = RecommendFilter.of(oddCount, sumMin, sumMax, disabledRules);
        recommendModelSupport.addRecommendModel(PublicQueryParams.normalizeCount(count), filter, model);
        return RECOMMEND_FRAGMENT;
    }
}
