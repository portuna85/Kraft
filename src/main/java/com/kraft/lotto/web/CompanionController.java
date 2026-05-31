package com.kraft.lotto.web;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
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
public class CompanionController {

    private static final String COMPANION_RESULT_FRAGMENT = "fragments/companion-result :: companion-result";

    private final WinningStatisticsService statisticsService;

    @GetMapping("/companion")
    public String companionPage(Model model) {
        model.addAttribute("allNumbers", allNumbers());
        return "companion";
    }

    @GetMapping("/fragments/companion")
    public String companionResult(
            @RequestParam @Min(1) @Max(45) int target,
            Model model
    ) {
        model.addAttribute("target", target);
        model.addAttribute("companions", statisticsService.companionNumbers(target));
        return COMPANION_RESULT_FRAGMENT;
    }

    private List<Integer> allNumbers() {
        return java.util.stream.IntStream.rangeClosed(LottoRoundPolicy.MIN_BALL, LottoRoundPolicy.MAX_BALL)
                .boxed().toList();
    }
}
