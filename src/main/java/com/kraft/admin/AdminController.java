package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import com.kraft.winningnumber.WinningNumberBackfillService;
import com.kraft.winningnumber.WinningNumberCollectionService;
import com.kraft.winningnumber.WinningNumberListResponse;
import com.kraft.winningnumber.WinningNumberQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final WinningNumberQueryService queryService;
    private final WinningNumberCollectionService collectionService;
    private final WinningNumberBackfillService backfillService;
    private final AdminAuditLogService auditLogService;
    private final ClientIpResolver ipResolver;

    public AdminController(WinningNumberQueryService queryService,
                           WinningNumberCollectionService collectionService,
                           WinningNumberBackfillService backfillService,
                           AdminAuditLogService auditLogService,
                           ClientIpResolver ipResolver) {
        this.queryService = queryService;
        this.collectionService = collectionService;
        this.backfillService = backfillService;
        this.auditLogService = auditLogService;
        this.ipResolver = ipResolver;
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        queryService.findLatest().ifPresent(w -> model.addAttribute("latest", AdminRoundView.from(w)));
        return "admin/dashboard";
    }

    @GetMapping("/rounds")
    public String rounds(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model) {
        WinningNumberListResponse list = queryService.list(page, Math.min(size, 100));
        var items = list.items().stream().map(AdminRoundView::from).toList();
        model.addAttribute("rounds", new AdminRoundPageView(items, list.page(), list.totalElements(), list.totalPages()));
        return "admin/rounds";
    }

    @PostMapping("/rounds/collect")
    public String collect(@RequestParam(required = false) Integer round,
                          @AuthenticationPrincipal UserDetails user,
                          HttpServletRequest req,
                          RedirectAttributes redirect) {
        try {
            if (round != null) {
                collectionService.collectRound(round);
                auditLogService.record(user.getUsername(), "COLLECT_ROUND",
                        "round=" + round, null, ipResolver.resolve(req));
                redirect.addFlashAttribute("success", round + "회차 수집 완료");
            } else {
                var resp = collectionService.collectLatest();
                auditLogService.record(user.getUsername(), "COLLECT_LATEST",
                        "round=" + resp.round(), null, ipResolver.resolve(req));
                redirect.addFlashAttribute("success", resp.round() + "회차 수집 완료");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "수집 실패: " + e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @PostMapping("/rounds/collect-all")
    public String collectAll(@AuthenticationPrincipal UserDetails user,
                             HttpServletRequest req,
                             RedirectAttributes redirect) {
        if (backfillService.isRunning()) {
            redirect.addFlashAttribute("error", "전체 회차 수집이 이미 진행 중입니다. 완료 후 다시 시도하세요.");
            return "redirect:/admin/rounds";
        }
        backfillService.backfillAllAsync();
        auditLogService.record(user.getUsername(), "COLLECT_ALL",
                "전체 회차 수집 시작", null, ipResolver.resolve(req));
        redirect.addFlashAttribute("success",
                "전체 회차 수집을 백그라운드에서 시작했습니다. 수 분이 소요되며, 잠시 후 회차 목록을 새로고침하세요.");
        return "redirect:/admin/rounds";
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        Model model) {
        model.addAttribute("logs", auditLogService.findAll(PageRequest.of(page, Math.min(size, 200))));
        return "admin/audit";
    }
}
