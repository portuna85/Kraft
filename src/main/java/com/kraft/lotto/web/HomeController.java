package com.kraft.lotto.web;

import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

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
            @RequestParam(required = false) @Min(LottoRoundPolicy.MIN_ROUND) @Max(LottoRoundPolicy.MAX_ROUND) Integer round,
            @RequestParam(defaultValue = "5") @Min(PublicQueryParams.MIN_COUNT) @Max(PublicQueryParams.MAX_COUNT) int count,
            Model model
    ) {
        addHomeModel(PublicQueryParams.normalizeRound(round), model);
        addRecommendModel(PublicQueryParams.normalizeCount(count), model);
        return "home";
    }

    @GetMapping("/recommend")
    public RedirectView recommendRedirect() {
        RedirectView rv = new RedirectView("/");
        rv.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
        return rv;
    }

    @GetMapping("/frequency")
    public String frequencyPage(
            @RequestParam(defaultValue = "0") @Min(0) @Max(200) int period,
            Model model
    ) {
        addFrequencyModel(period, model);
        return "frequency";
    }

    @GetMapping("/rounds")
    public String roundsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(LottoRoundPolicy.MIN_ROUND) @Max(LottoRoundPolicy.MAX_ROUND) Integer round,
            Model model
    ) {
        int safePage = PublicQueryParams.normalizePage(page);
        int safeSize = PublicQueryParams.normalizeSize(size);
        var rounds = queryService.list(safePage, safeSize);
        model.addAttribute("rounds", rounds);
        model.addAttribute("page", rounds.page());
        model.addAttribute("size", rounds.size());
        model.addAttribute("pageSizes", List.of(20, 50, 100));
        model.addAttribute("currentSize", safeSize);
        model.addAttribute("maxRound", LottoRoundPolicy.MAX_ROUND);
        model.addAttribute("round", round);
        model.addAttribute("result", round == null ? null : queryService.findByRound(round).orElse(null));
        return "rounds";
    }

    @GetMapping("/fragments/recommend")
    public String recommend(
            @RequestParam(defaultValue = "5") @Min(PublicQueryParams.MIN_COUNT) @Max(PublicQueryParams.MAX_COUNT) int count,
            Model model
    ) {
        addRecommendModel(PublicQueryParams.normalizeCount(count), model);
        return RECOMMEND_FRAGMENT;
    }

    @GetMapping("/fragments/frequency")
    public String frequency(
            @RequestParam(defaultValue = "0") @Min(0) @Max(200) int period,
            Model model
    ) {
        addFrequencyModel(period, model);
        return FREQUENCY_FRAGMENT;
    }

    @GetMapping("/fragments/rounds")
    public String rounds(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Model model
    ) {
        int safePage = PublicQueryParams.normalizePage(page);
        int safeSize = PublicQueryParams.normalizeSize(size);
        var rounds = queryService.list(safePage, safeSize);
        model.addAttribute("rounds", rounds);
        model.addAttribute("page", rounds.page());
        model.addAttribute("size", rounds.size());
        model.addAttribute("pageSizes", List.of(20, 50, 100));
        model.addAttribute("currentSize", safeSize);
        return ROUNDS_FRAGMENT;
    }

    private void addFrequencyModel(int period, Model model) {
        var summary = statisticsService.frequencySummary();
        List<NumberFrequencyDto> rawFrequencies = period > 0
                ? statisticsService.frequencyForPeriod(period)
                : summary.frequencies();
        List<FrequencyViewModel> freqVMs = toFrequencyViewModels(rawFrequencies);

        List<FrequencyViewModel> topBalls = freqVMs.stream()
                .filter(f -> f.rank() <= 6)
                .sorted(Comparator.comparingInt(FrequencyViewModel::number))
                .toList();
        FrequencyViewModel topBonus = freqVMs.stream()
                .filter(f -> f.rank() == 7)
                .findFirst()
                .orElse(null);

        List<FrequencyViewModel> bottomBalls = freqVMs.stream()
                .filter(f -> f.rank() >= 40)
                .sorted(Comparator.comparingInt(FrequencyViewModel::number))
                .toList();
        FrequencyViewModel bottomBonus = freqVMs.stream()
                .filter(f -> f.rank() == 39)
                .findFirst()
                .orElse(null);

        List<Integer> top6sorted = topBalls.stream().map(FrequencyViewModel::number).sorted().toList();

        model.addAttribute("frequency", freqVMs);
        model.addAttribute("topBalls", topBalls);
        model.addAttribute("topBonus", topBonus);
        model.addAttribute("bottomBalls", bottomBalls);
        model.addAttribute("bottomBonus", bottomBonus);
        model.addAttribute("top6History", statisticsService.combinationPrizeHistory(top6sorted));
        model.addAttribute("bottom6History", summary.lowSixCombinationHistory());
        model.addAttribute("period", period);
    }

    private void addHomeModel(Integer round, Model model) {
        model.addAttribute("expectedRound", queryService.expectedCurrentRound());
        model.addAttribute("latest", queryService.findLatest().orElse(null));
        model.addAttribute("result", round == null ? null : queryService.findByRound(round).orElse(null));
        model.addAttribute("maxRound", LottoRoundPolicy.MAX_ROUND);
    }

    private void addRecommendModel(int count, Model model) {
        var recommendation = recommendService.recommend(count);
        model.addAttribute("count", count);
        model.addAttribute("combinations", recommendation.combinations());
        model.addAttribute("rules", recommendService.rules());
        model.addAttribute("maxCount", PublicQueryParams.MAX_COUNT);
    }

    private static List<FrequencyViewModel> toFrequencyViewModels(List<NumberFrequencyDto> frequencies) {
        long max = Math.max(1L, frequencies.stream().mapToLong(NumberFrequencyDto::count).max().orElse(1L));
        List<NumberFrequencyDto> byCount = frequencies.stream()
                .sorted(java.util.Comparator.comparingLong(NumberFrequencyDto::count).reversed())
                .toList();
        Map<Integer, Integer> rankByNumber = new HashMap<>(byCount.size() * 2);
        for (int i = 0; i < byCount.size(); i++) {
            rankByNumber.put(byCount.get(i).number(), i + 1);
        }
        List<FrequencyViewModel> result = new ArrayList<>(frequencies.size());
        for (NumberFrequencyDto f : frequencies) {
            double percent = f.count() * 100.0 / max;
            int rank = rankByNumber.getOrDefault(f.number(), frequencies.size());
            result.add(new FrequencyViewModel(f.number(), f.count(), percent, rank));
        }
        return result;
    }
}
