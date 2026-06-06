package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.support.ClientIpResolver;
import com.kraft.lotto.web.OpsCollectionFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
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
    public String collectLatest(Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
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
                                Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        boolean saved = winningStoreCollector.collectStores(round);
        auditLogService.recordSuccess(actor, "COLLECT_STORES",
                "round:" + round + ",saved:" + saved, ip, request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message",
                round + "회차 판매점 수집 " + (saved ? "완료" : "스킵(이미 존재)"));
        return "redirect:/admin/ops/collection";
    }

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}
