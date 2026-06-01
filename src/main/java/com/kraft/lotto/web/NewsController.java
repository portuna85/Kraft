package com.kraft.lotto.web;

import com.kraft.lotto.feature.news.application.NewsQueryService;
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
            Model model
    ) {
        NewsQueryService.NewsPage result = newsQueryService.list(page, size);
        model.addAttribute("articles", result.articles());
        model.addAttribute("page", result.page());
        model.addAttribute("size", result.size());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("totalPages", result.totalPages());
        return "news";
    }
}
