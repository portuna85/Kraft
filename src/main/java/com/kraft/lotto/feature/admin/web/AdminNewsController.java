package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminNewsService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Validated
@Controller
@RequestMapping("/admin/ops/news")
public class AdminNewsController {

    private final AdminNewsService adminNewsService;

    public AdminNewsController(AdminNewsService adminNewsService) {
        this.adminNewsService = adminNewsService;
    }

    @GetMapping
    public String newsList(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(5) @Max(100) int pageSize,
            @RequestParam(defaultValue = "pending") String tab,
            Model model) {
        var pageable = PageRequest.of(page, pageSize);
        model.addAttribute("tab", tab);
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        switch (tab) {
            case "approved" -> model.addAttribute("articles", adminNewsService.listApproved(pageable));
            case "rejected" -> model.addAttribute("articles", adminNewsService.listRejected(pageable));
            case "blocked" -> {
                model.addAttribute("blockedDomains", adminNewsService.listBlockedDomains());
                model.addAttribute("blockedKeywords", adminNewsService.listBlockedKeywords());
            }
            default -> model.addAttribute("articles", adminNewsService.listPending(pageable));
        }
        return "admin/news";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable long id,
                          Principal principal,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, List.of());
        adminNewsService.approve(id, actor, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", "기사 승인 완료");
        return "redirect:/admin/ops/news";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable long id,
                         Principal principal,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, List.of());
        adminNewsService.reject(id, actor, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", "기사 거부 완료");
        return "redirect:/admin/ops/news";
    }

    @PostMapping("/{id}/block-domain")
    public String blockDomain(@PathVariable long id,
                              @RequestParam(required = false) String reason,
                              Principal principal,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, List.of());
        adminNewsService.blockDomain(id, reason, actor, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", "도메인 차단 완료");
        return "redirect:/admin/ops/news";
    }

    @PostMapping("/block-keyword")
    public String blockKeyword(@RequestParam String keyword,
                               @RequestParam(required = false) String reason,
                               Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, List.of());
        adminNewsService.blockKeyword(keyword, reason, actor, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", "키워드 차단 완료: " + keyword);
        return "redirect:/admin/ops/news";
    }

    @GetMapping("/reclassify")
    @ResponseBody
    public ResponseEntity<ReclassifyCountResponse> reclassifyDryRun(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        int count = adminNewsService.countReclassifiable(days);
        return ResponseEntity.ok(new ReclassifyCountResponse(count, days));
    }

    @PostMapping("/reclassify")
    @ResponseBody
    public ResponseEntity<ReclassifyApplyResponse> reclassifyApply(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
            Principal principal,
            HttpServletRequest request) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, List.of());
        int reclassified = adminNewsService.reclassifyApproved(days);
        adminNewsService.recordReclassifyAudit(actor, ip, request.getHeader("User-Agent"), days, reclassified);
        return ResponseEntity.ok(new ReclassifyApplyResponse(reclassified, days));
    }

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }

    public record ReclassifyCountResponse(int count, int days) {}
    public record ReclassifyApplyResponse(int reclassified, int days) {}
}
