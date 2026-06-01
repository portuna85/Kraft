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

    private static final double TAX_HIGH = 0.33;   // 3억 초과: 소득세 30% + 지방소득세 3%
    private static final double TAX_MID  = 0.22;   // 200만 초과 ~ 3억 이하
    private static final long   THRESHOLD_HIGH = 300_000_000L;
    private static final long   THRESHOLD_MID  =   2_000_000L;

    private final WinningNumberQueryService queryService;

    @GetMapping("/latest")
    public String latestPage(Model model) {
        queryService.findLatest().ifPresent(wn -> addModel(wn, model));
        return "latest";
    }

    private void addModel(WinningNumberDto wn, Model model) {
        model.addAttribute("latest", wn);
        model.addAttribute("firstAfterTax",  afterTax(wn.firstPrize()));
        model.addAttribute("secondAfterTax", afterTax(wn.secondPrize()));
        model.addAttribute("dhLotteryUrl",
                "https://www.dhlottery.co.kr/gameResult.do?method=byWin&drwNo=" + wn.round());
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
        return (raw / 1_000_000L) * 1_000_000L;
    }
}
