package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningStoreQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningRegionSummaryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningStoreDto;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotto/draws")
@RequiredArgsConstructor
public class LottoDrawWinningStoreController {

    private final WinningStoreQueryService queryService;

    @GetMapping("/{drawNo}/winning-stores")
    public List<WinningStoreDto> getWinningStores(
            @PathVariable int drawNo,
            @RequestParam(required = false) Integer grade) {
        if (grade != null) {
            return queryService.findByRoundAndGrade(drawNo, grade);
        }
        return Stream.concat(
                queryService.findByRoundAndGrade(drawNo, 1).stream(),
                queryService.findByRoundAndGrade(drawNo, 2).stream()
        ).toList();
    }

    @GetMapping("/{drawNo}/winning-regions")
    public List<WinningRegionSummaryDto> getWinningRegions(@PathVariable int drawNo) {
        return queryService.findRegionSummary(drawNo);
    }
}
