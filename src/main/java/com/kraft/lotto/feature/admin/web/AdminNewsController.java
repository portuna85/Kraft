package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminNewsService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}
