package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PublicPageController {

    private final WinningStatisticsService statisticsService;

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("stats", statisticsService.patternStats());
        return "stats";
    }
}
