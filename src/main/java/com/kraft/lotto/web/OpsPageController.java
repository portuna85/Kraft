package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class OpsPageController {

    private final LottoFetchLogQueryService fetchLogQueryService;

    @GetMapping("/admin/ops")
    public String opsDashboard(
            @RequestParam(defaultValue = "200") @Min(1) @Max(2000) int reasonLimit,
            @RequestParam(defaultValue = "100") @Min(1) @Max(2000) int logLimit,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(5) @Max(100) int pageSize,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            Model model
    ) {
        int safeReasonLimit = OpsQueryParams.normalizeReasonLimit(reasonLimit);
        int safeLogLimit = OpsQueryParams.normalizeLogLimit(logLimit);
        int safePage = PublicQueryParams.normalizePage(page);
        int safePageSize = PublicQueryParams.normalizeSize(pageSize);
        String safeReason = OpsQueryParams.normalizeReason(reason);
        OpsQueryParams.Range range = OpsQueryParams.normalizeRange(drwNoFrom, drwNoTo);

        FetchFailureOverviewDto overview = fetchLogQueryService.failureOverview(
                safeReasonLimit,
                safeLogLimit,
                safeReason,
                range.from(),
                range.to()
        );
        var pagedFailures = fetchLogQueryService.listRecentFailuresPage(
                safePage,
                safePageSize,
                safeReason,
                range.from(),
                range.to()
        );

        model.addAttribute("overview", overview);
        model.addAttribute("pagedFailures", pagedFailures.rows());
        model.addAttribute("reason", safeReason == null ? "" : safeReason);
        model.addAttribute("drwNoFrom", range.from());
        model.addAttribute("drwNoTo", range.to());
        model.addAttribute("reasonLimit", safeReasonLimit);
        model.addAttribute("logLimit", safeLogLimit);
        model.addAttribute("page", pagedFailures.page());
        model.addAttribute("pageSize", pagedFailures.pageSize());
        model.addAttribute("hasNext", pagedFailures.hasNext());
        return "admin-ops";
    }
}
