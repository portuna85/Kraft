package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class AnalysisController {

    private static final String ANALYSIS_RESULT_FRAGMENT = "fragments/analysis-result :: analysis-result";
    private static final int LOTTO_MIN = LottoRoundPolicy.MIN_BALL;
    private static final int LOTTO_MAX = LottoRoundPolicy.MAX_BALL;

    private final WinningStatisticsService statisticsService;

    @GetMapping("/analysis")
    public String analysisPage(Model model) {
        model.addAttribute("allNumbers", allNumbers());
        return "analysis";
    }

    @GetMapping("/fragments/analysis")
    public String analysisResult(
            @RequestParam @Min(1) @Max(45) int n1,
            @RequestParam @Min(1) @Max(45) int n2,
            @RequestParam @Min(1) @Max(45) int n3,
            @RequestParam @Min(1) @Max(45) int n4,
            @RequestParam @Min(1) @Max(45) int n5,
            @RequestParam @Min(1) @Max(45) int n6,
            Model model
    ) {
        List<Integer> numbers = List.of(n1, n2, n3, n4, n5, n6).stream().sorted().toList();
        if (Set.of(n1, n2, n3, n4, n5, n6).size() < 6) {
            model.addAttribute("error", "중복된 번호가 있습니다.");
            return ANALYSIS_RESULT_FRAGMENT;
        }

        List<NumberFrequencyDto> allFreq = statisticsService.frequency();
        List<FrequencyViewModel> selectedVMs = toFrequencyViewModels(allFreq).stream()
                .filter(f -> numbers.contains(f.number()))
                .sorted(Comparator.comparingInt(FrequencyViewModel::number))
                .toList();

        model.addAttribute("selectedBalls", selectedVMs);
        model.addAttribute("history", statisticsService.combinationPrizeHistory(numbers));
        return ANALYSIS_RESULT_FRAGMENT;
    }

    private List<Integer> allNumbers() {
        return java.util.stream.IntStream.rangeClosed(LOTTO_MIN, LOTTO_MAX)
                .boxed().toList();
    }

    private static List<FrequencyViewModel> toFrequencyViewModels(List<NumberFrequencyDto> frequencies) {
        long max = Math.max(1L, frequencies.stream().mapToLong(NumberFrequencyDto::count).max().orElse(1L));
        List<NumberFrequencyDto> byCount = frequencies.stream()
                .sorted(Comparator.comparingLong(NumberFrequencyDto::count).reversed())
                .toList();
        return frequencies.stream().map(f -> {
            double pct = f.count() * 100.0 / max;
            int rank = byCount.indexOf(f) + 1;
            return new FrequencyViewModel(f.number(), f.count(), pct, rank);
        }).toList();
    }
}
