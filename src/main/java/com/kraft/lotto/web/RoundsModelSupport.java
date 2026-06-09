package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class RoundsModelSupport {

    private final WinningNumberQueryService queryService;

    void addRoundsListModel(int page, int size, Model model) {
        int safePage = PublicQueryParams.normalizePage(page);
        int safeSize = PublicQueryParams.normalizeSize(size);
        var rounds = queryService.list(safePage, safeSize);
        model.addAttribute("rounds", rounds);
        model.addAttribute("page", rounds.page());
        model.addAttribute("size", rounds.size());
        model.addAttribute("pageSizes", List.of(20, 50, 100));
        model.addAttribute("currentSize", safeSize);
        if (rounds.page() > 0) {
            model.addAttribute("prevLink", "/rounds?page=" + (rounds.page() - 1) + "&size=" + rounds.size());
        }
        if (rounds.page() + 1 < rounds.totalPages()) {
            model.addAttribute("nextLink", "/rounds?page=" + (rounds.page() + 1) + "&size=" + rounds.size());
        }
    }

    void addRoundSearchModel(Integer round, Model model) {
        model.addAttribute("maxRound", queryService.userSearchMaxRound());
        model.addAttribute("round", round);
        var result = round == null ? null : queryService.findByRound(round).orElse(null);
        model.addAttribute("result", result);
    }
}
