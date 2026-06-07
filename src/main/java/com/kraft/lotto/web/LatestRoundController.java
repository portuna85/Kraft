package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LatestRoundController {

    private final WinningNumberQueryService queryService;
    private final WinningStoreQueryService  storeQueryService;

    @GetMapping("/latest")
    public String latestPage(Model model) {
        queryService.findLatest().ifPresent(wn -> addModel(wn, model));
        return "latest";
    }

    private void addModel(WinningNumberDto wn, Model model) {
        long firstAfterTax  = LottoPrizeTaxCalculator.afterTax(wn.firstPrize());
        long secondAfterTax = LottoPrizeTaxCalculator.afterTax(wn.secondPrize());
        boolean store1Collected = storeQueryService.hasGrade(wn.round(), 1);
        boolean store2Collected = storeQueryService.hasGrade(wn.round(), 2);
        model.addAttribute("latest", wn);
        model.addAttribute("firstAfterTax",    firstAfterTax);
        model.addAttribute("secondAfterTax",   secondAfterTax);
        model.addAttribute("firstTax",         wn.firstPrize()  - firstAfterTax);
        model.addAttribute("secondTax",        wn.secondPrize() - secondAfterTax);
        model.addAttribute("firstTaxRate",     LottoPrizeTaxCalculator.taxRate(wn.firstPrize()));
        model.addAttribute("secondTaxRate",    LottoPrizeTaxCalculator.taxRate(wn.secondPrize()));
        model.addAttribute("firstTotalPrize",  (long) wn.firstPrize()  * wn.firstWinners());
        model.addAttribute("secondTotalPrize", (long) wn.secondPrize() * wn.secondWinners());
        var allStores = storeQueryService.findRegionSummary(wn.round());
        model.addAttribute("stores1",          storeQueryService.findByRoundAndGrade(wn.round(), 1));
        model.addAttribute("stores2",          storeQueryService.findByRoundAndGrade(wn.round(), 2));
        model.addAttribute("regions1",         allStores.stream().filter(r -> r.grade() == 1).toList());
        model.addAttribute("regions2",         allStores.stream().filter(r -> r.grade() == 2).toList());
        model.addAttribute("storesCollected",  store1Collected || store2Collected);
        model.addAttribute("store1Collected",  store1Collected);
        model.addAttribute("store2Collected",  store2Collected);
        model.addAttribute("storeCollectedAt", storeQueryService.findLastCollectedAt(wn.round()).orElse(null));
    }
}
