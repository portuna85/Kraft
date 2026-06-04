package com.kraft.lotto.web;

import com.kraft.lotto.feature.news.application.NewsQueryService;
import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class NewsController {

    private static final int DEFAULT_SIZE = 20;

    private final NewsQueryService newsQueryService;

    @GetMapping("/news")
    public String news(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) @Max(50) int size,
            @RequestParam(required = false) String tier,
            Model model
    ) {
        NewsSourceTier selectedTier = parseTier(tier);
        NewsQueryService.NewsPage result = newsQueryService.list(page, size, selectedTier);
        model.addAttribute("articles", result.articles());
        model.addAttribute("page", result.page());
        model.addAttribute("size", result.size());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("tiers", NewsSourceTier.values());
        model.addAttribute("currentTier", selectedTier == null ? "" : selectedTier.paramValue());
        return "news";
    }

    private static NewsSourceTier parseTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return null;
        }
        return NewsSourceTier.fromParam(tier)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "tier must be one of official, press, general"
                ));
    }
}
