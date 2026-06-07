package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningRegionSummaryDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class RoundsModelSupport {

    private final WinningNumberQueryService queryService;
    private final WinningStoreQueryService storeQueryService;

    void addRoundsListModel(int page, int size, Model model) {
        int safePage = PublicQueryParams.normalizePage(page);
        int safeSize = PublicQueryParams.normalizeSize(size);
        var rounds = queryService.list(safePage, safeSize);
        model.addAttribute("rounds", rounds);
        model.addAttribute("page", rounds.page());
        model.addAttribute("size", rounds.size());
        model.addAttribute("pageSizes", List.of(20, 50, 100));
        model.addAttribute("currentSize", safeSize);
    }

    void addRoundSearchModel(Integer round, Model model) {
        model.addAttribute("maxRound", queryService.maxPossibleRound());
        model.addAttribute("round", round);
        var result = round == null ? null : queryService.findByRound(round).orElse(null);
        model.addAttribute("result", result);
        if (result != null) {
            var allRegions = storeQueryService.findRegionSummary(result.round());
            model.addAttribute("searchStores1",  storeQueryService.findByRoundAndGrade(result.round(), 1));
            model.addAttribute("searchStores2",  storeQueryService.findByRoundAndGrade(result.round(), 2));
            model.addAttribute("searchRegions1", allRegions.stream().filter(r -> r.grade() == 1).toList());
            model.addAttribute("searchRegions2", allRegions.stream().filter(r -> r.grade() == 2).toList());
            model.addAttribute("searchStoresCollected", storeQueryService.hasStores(result.round()));
        }
    }
}
