package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class OpsPageController {

    private final LottoFetchLogQueryService fetchLogQueryService;

    @GetMapping("/admin/ops")
    public String opsDashboard(
            @RequestParam(defaultValue = "200") int reasonLimit,
            @RequestParam(defaultValue = "100") int logLimit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer drwNoFrom,
            @RequestParam(required = false) Integer drwNoTo,
            HttpServletRequest request,
            Model model
    ) {
        int safeReasonLimit = OpsQueryParams.normalizeLimit(reasonLimit);
        int safeLogLimit = OpsQueryParams.normalizeLimit(logLimit);
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(5, Math.min(pageSize, 100));
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
