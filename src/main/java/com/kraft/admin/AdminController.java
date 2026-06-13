package com.kraft.admin;

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
    private final AdminAuditLogService auditLogService;

    public AdminController(WinningNumberQueryService queryService,
                           WinningNumberCollectionService collectionService,
                           AdminAuditLogService auditLogService) {
        this.queryService = queryService;
        this.collectionService = collectionService;
        this.auditLogService = auditLogService;
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
        model.addAttribute("latest", queryService.getLatest());
        return "admin/dashboard";
    }

    @GetMapping("/rounds")
    public String rounds(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model) {
        WinningNumberListResponse list = queryService.list(page, Math.min(size, 100));
        model.addAttribute("rounds", list);
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
                        "round=" + round, null, req.getRemoteAddr());
                redirect.addFlashAttribute("success", round + "회차 수집 완료");
            } else {
                var resp = collectionService.collectLatest();
                auditLogService.record(user.getUsername(), "COLLECT_LATEST",
                        "round=" + resp.round(), null, req.getRemoteAddr());
                redirect.addFlashAttribute("success", resp.round() + "회차 수집 완료");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "수집 실패: " + e.getMessage());
        }
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
