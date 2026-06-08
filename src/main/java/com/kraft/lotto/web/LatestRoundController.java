package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LatestRoundController {

    private final WinningNumberQueryService queryService;

    @GetMapping("/latest")
    public String latestPage(Model model) {
        var latest = queryService.findLatest().orElse(null);
        if (latest != null) {
            addModel(latest, model);
            model.addAttribute("pageTitle",
                latest.round() + "회 로또 당첨번호 (" + latest.drawDate() + ")");
            model.addAttribute("pageDescription",
                latest.round() + "회(" + latest.drawDate() + ") 당첨번호와 세후 예상 수령액.");
        } else {
            model.addAttribute("pageTitle", "최신 로또 당첨번호");
            model.addAttribute("pageDescription", "최신 로또 당첨번호를 확인하세요.");
        }
        return "latest";
    }

    private void addModel(WinningNumberDto wn, Model model) {
        long firstAfterTax  = LottoPrizeTaxCalculator.afterTax(wn.firstPrize());
        long secondAfterTax = LottoPrizeTaxCalculator.afterTax(wn.secondPrize());
        model.addAttribute("latest", wn);
        model.addAttribute("firstAfterTax",    firstAfterTax);
        model.addAttribute("secondAfterTax",   secondAfterTax);
        model.addAttribute("firstTax",         wn.firstPrize()  - firstAfterTax);
        model.addAttribute("secondTax",        wn.secondPrize() - secondAfterTax);
        model.addAttribute("firstTaxRate",     LottoPrizeTaxCalculator.taxRate(wn.firstPrize()));
        model.addAttribute("secondTaxRate",    LottoPrizeTaxCalculator.taxRate(wn.secondPrize()));
        model.addAttribute("firstTotalPrize",  (long) wn.firstPrize()  * wn.firstWinners());
        model.addAttribute("secondTotalPrize", (long) wn.secondPrize() * wn.secondWinners());
    }
}
