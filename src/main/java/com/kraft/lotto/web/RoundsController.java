package com.kraft.lotto.web;

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
public class RoundsController {

    private static final String ROUNDS_FRAGMENT = "fragments/rounds-list :: rounds-list";

    private final RoundsModelSupport roundsModelSupport;

    @GetMapping("/rounds")
    public String roundsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(LottoRoundPolicy.MIN_ROUND) @Max(LottoRoundPolicy.MAX_ROUND) Integer round,
            Model model
    ) {
        roundsModelSupport.addRoundsListModel(page, size, model);
        roundsModelSupport.addRoundSearchModel(round, model);
        return "rounds";
    }

    @GetMapping("/fragments/rounds")
    public String rounds(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Model model
    ) {
        roundsModelSupport.addRoundsListModel(page, size, model);
        return ROUNDS_FRAGMENT;
    }
}
