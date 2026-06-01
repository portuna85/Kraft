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

    private static final double TAX_HIGH = 0.33;
    private static final double TAX_MID  = 0.22;
    private static final long   THRESHOLD_HIGH = 300_000_000L;
    private static final long   THRESHOLD_MID  =   2_000_000L;

    private final WinningNumberQueryService queryService;
    private final WinningStoreQueryService  storeQueryService;

    @GetMapping("/latest")
    public String latestPage(Model model) {
        queryService.findLatest().ifPresent(wn -> addModel(wn, model));
        return "latest";
    }

    private void addModel(WinningNumberDto wn, Model model) {
        long firstAfterTax  = afterTax(wn.firstPrize());
        long secondAfterTax = afterTax(wn.secondPrize());
        model.addAttribute("latest", wn);
        model.addAttribute("firstAfterTax",    firstAfterTax);
        model.addAttribute("secondAfterTax",   secondAfterTax);
        model.addAttribute("firstTax",         wn.firstPrize()  - firstAfterTax);
        model.addAttribute("secondTax",        wn.secondPrize() - secondAfterTax);
        model.addAttribute("firstTaxRate",     taxRate(wn.firstPrize()));
        model.addAttribute("secondTaxRate",    taxRate(wn.secondPrize()));
        model.addAttribute("firstTotalPrize",  (long) wn.firstPrize()  * wn.firstWinners());
        model.addAttribute("secondTotalPrize", (long) wn.secondPrize() * wn.secondWinners());
        model.addAttribute("stores1",          storeQueryService.findByRoundAndGrade(wn.round(), 1));
        model.addAttribute("stores2",          storeQueryService.findByRoundAndGrade(wn.round(), 2));
        model.addAttribute("storesCollected",  storeQueryService.hasStores(wn.round()));
    }

    static long afterTax(long prize) {
        if (prize <= 0) {
            return 0;
        }
        long raw;
        if (prize > THRESHOLD_HIGH) {
            raw = (long) (prize * (1.0 - TAX_HIGH));
        } else if (prize > THRESHOLD_MID) {
            raw = (long) (prize * (1.0 - TAX_MID));
        } else {
            raw = prize;
        }
        return raw;
    }

    static String taxRate(long prize) {
        if (prize > THRESHOLD_HIGH) {
            return "33%";
        }
        if (prize > THRESHOLD_MID) {
            return "22%";
        }
        return "-";
    }
}
