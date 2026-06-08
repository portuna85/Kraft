package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminAuditLogService.AuditFilter;
import com.kraft.lotto.support.ClientIpResolver;
import com.kraft.lotto.web.OpsCollectionFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ops/collection")
public class AdminCollectionController {

    private final OpsCollectionFacade opsCollectionFacade;
    private final AdminAuditLogService auditLogService;

    public AdminCollectionController(OpsCollectionFacade opsCollectionFacade,
                                     AdminAuditLogService auditLogService) {
        this.opsCollectionFacade = opsCollectionFacade;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String collectionPage(Model model) {
        var filter = new AuditFilter("COLLECT", null, null, null);
        model.addAttribute("recentHistory", auditLogService.list(filter, PageRequest.of(0, 15)));
        return "admin/collection";
    }

    @PostMapping("/latest")
    public String collectLatest(Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        String ua = request.getHeader("User-Agent");
        try {
            var result = opsCollectionFacade.collectLatest(null, ip);
            auditLogService.recordSuccess(actor, "COLLECT_LATEST",
                    "collected:" + result.collected(), ip, ua);
            redirectAttributes.addFlashAttribute("message",
                    "수집 완료: " + result.collected() + "건");
        } catch (RuntimeException e) {
            auditLogService.recordFailure(actor, "COLLECT_LATEST", "latest", ip, ua, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "수집 실패: " + e.getMessage());
        }
        return "redirect:/admin/ops/collection";
    }

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}
