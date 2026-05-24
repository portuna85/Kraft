package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String RECOMMEND_FRAGMENT = "fragments/recommend-card :: recommend-card";
    private static final String FREQUENCY_FRAGMENT = "fragments/frequency-card :: frequency-card";
    private static final String ROUNDS_FRAGMENT = "fragments/rounds-list :: rounds-list";

    private final WinningNumberQueryService queryService;
    private final RecommendService recommendService;
    private final WinningStatisticsService statisticsService;

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) @Min(1) @Max(3000) Integer round,
            Model model
    ) {
        addHomeModel(round, model);
        return "home";
    }

    @GetMapping("/fragments/recommend")
    public String recommend(
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int count,
            @RequestParam(required = false) @Min(1) @Max(3000) Integer round,
            Model model
    ) {
        addRecommendModel(count, round, model);
        return RECOMMEND_FRAGMENT;
    }

    @GetMapping("/fragments/frequency")
    public String frequency(Model model) {
        List<NumberFrequencyDto> frequencies = statisticsService.frequency();
        model.addAttribute("frequency", frequencies);
        model.addAttribute("maxFreq", maxFrequency(frequencies));
        return FREQUENCY_FRAGMENT;
    }

    @GetMapping("/fragments/rounds")
    public String rounds(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Model model
    ) {
        var rounds = queryService.list(page, size);
        model.addAttribute("rounds", rounds);
        model.addAttribute("page", rounds.page());
        model.addAttribute("size", rounds.size());
        return ROUNDS_FRAGMENT;
    }

    private void addHomeModel(Integer round, Model model) {
        model.addAttribute("expectedRound", queryService.expectedCurrentRound());
        model.addAttribute("latest", queryService.findLatest().orElse(null));
        model.addAttribute("result", round == null ? null : queryService.findByRound(round).orElse(null));
    }

    private void addRecommendModel(int count, Integer round, Model model) {
        var recommendation = recommendService.recommend(count);
        model.addAttribute("count", count);
        model.addAttribute("combinations", recommendation.combinations());
        model.addAttribute("rules", recommendService.rules());
        model.addAttribute("round", round);
    }

    private static long maxFrequency(List<NumberFrequencyDto> frequencies) {
        return Math.max(1L, frequencies.stream()
                .mapToLong(NumberFrequencyDto::count)
                .max()
                .orElse(1L));
    }
}
