package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
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
public class HomeController {

    private final WinningNumberQueryService queryService;

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) @Min(LottoRoundPolicy.MIN_ROUND) @Max(LottoRoundPolicy.MAX_ROUND) Integer round,
            Model model
    ) {
        addHomeModel(PublicQueryParams.normalizeRound(round), model);
        return "home";
    }

    private void addHomeModel(Integer round, Model model) {
        model.addAttribute("expectedRound", queryService.expectedCurrentRound());
        model.addAttribute("latest", queryService.findLatest().orElse(null));
        model.addAttribute("result", round == null ? null : queryService.findByRound(round).orElse(null));
        model.addAttribute("maxRound", queryService.maxPossibleRound());
    }

}
