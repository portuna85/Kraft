package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StatsController {

    private static final String STATS_FRAGMENT = "fragments/stats-card :: stats-card";

    private final WinningStatisticsService statisticsService;

    @GetMapping("/stats")
    public String statsPage(Model model) {
        addStatsModel(model);
        return "stats";
    }

    @GetMapping("/fragments/stats")
    public String statsFragment(Model model) {
        addStatsModel(model);
        return STATS_FRAGMENT;
    }

    private void addStatsModel(Model model) {
        model.addAttribute("stats", statisticsService.patternStats());
    }
}
