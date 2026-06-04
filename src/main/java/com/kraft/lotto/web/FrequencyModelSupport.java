package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FrequencySummaryDto;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class FrequencyModelSupport {

    private final WinningStatisticsService statisticsService;

    void addFrequencyModel(int period, Model model) {
        FrequencySummaryDto summary = period > 0 ? null : statisticsService.frequencySummary();
        var rawFrequencies = period > 0 ? statisticsService.frequencyForPeriod(period) : summary.frequencies();
        List<FrequencyViewModel> freqVMs = FrequencyViewModel.from(rawFrequencies);

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
        List<Integer> bottom6sorted = bottomBalls.stream().map(FrequencyViewModel::number).sorted().toList();
        CombinationPrizeHistoryDto bottom6History = summary == null
                ? statisticsService.combinationPrizeHistory(bottom6sorted)
                : summary.lowSixCombinationHistory();

        model.addAttribute("frequency", freqVMs);
        model.addAttribute("topBalls", topBalls);
        model.addAttribute("topBonus", topBonus);
        model.addAttribute("bottomBalls", bottomBalls);
        model.addAttribute("bottomBonus", bottomBonus);
        model.addAttribute("top6History", statisticsService.combinationPrizeHistory(top6sorted));
        model.addAttribute("bottom6History", bottom6History);
        model.addAttribute("period", period);
    }
}
