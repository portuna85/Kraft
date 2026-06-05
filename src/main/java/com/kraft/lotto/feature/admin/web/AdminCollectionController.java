package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.support.ClientIpResolver;
import com.kraft.lotto.web.OpsCollectionFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ops/collection")
public class AdminCollectionController {

    private final OpsCollectionFacade opsCollectionFacade;
    private final WinningStoreCollector winningStoreCollector;
    private final AdminAuditLogService auditLogService;

    public AdminCollectionController(OpsCollectionFacade opsCollectionFacade,
                                     WinningStoreCollector winningStoreCollector,
                                     AdminAuditLogService auditLogService) {
        this.opsCollectionFacade = opsCollectionFacade;
        this.winningStoreCollector = winningStoreCollector;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String collectionPage() {
        return "admin/collection";
    }

    @PostMapping("/latest")
    public String collectLatest(@AuthenticationPrincipal OAuth2User user,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractEmail(user);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        var result = opsCollectionFacade.collectLatest(null, ip);
        auditLogService.recordSuccess(actor, "COLLECT_LATEST",
                "collected:" + result.collected(), ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message",
                "수집 완료: " + result.collected() + "건");
        return "redirect:/admin/ops/collection";
    }

    @PostMapping("/stores")
    public String collectStores(@RequestParam int round,
                                @AuthenticationPrincipal OAuth2User user,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractEmail(user);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        boolean saved = winningStoreCollector.collectStores(round);
        auditLogService.recordSuccess(actor, "COLLECT_STORES",
                "round:" + round + ",saved:" + saved, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message",
                round + "회차 판매점 수집 " + (saved ? "완료" : "스킵(이미 존재)"));
        return "redirect:/admin/ops/collection";
    }

    private static String extractEmail(OAuth2User user) {
        if (user == null) {
            return "unknown";
        }
        Object email = user.getAttributes().get("email");
        return email != null ? email.toString() : "unknown";
    }
}
