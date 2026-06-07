package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminAuditLogService.AuditFilter;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreCollector;
import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import com.kraft.lotto.support.ClientIpResolver;
import com.kraft.lotto.web.OpsCollectionFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
        } catch (Exception e) {
            auditLogService.recordFailure(actor, "COLLECT_LATEST", "latest", ip, ua, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "수집 실패: " + e.getMessage());
        }
        return "redirect:/admin/ops/collection";
    }

    @PostMapping("/stores")
    public String collectStores(@RequestParam int round,
                                Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        String ua = request.getHeader("User-Agent");
        try {
            boolean saved = winningStoreCollector.collectStores(round);
            auditLogService.recordSuccess(actor, "COLLECT_STORES",
                    "round:" + round + ",saved:" + saved, ip, ua);
            redirectAttributes.addFlashAttribute("message",
                    round + "회차 판매점 수집 " + (saved ? "완료" : "스킵(이미 존재)"));
        } catch (Exception e) {
            auditLogService.recordFailure(actor, "COLLECT_STORES", "round:" + round, ip, ua, e.getMessage());
            redirectAttributes.addFlashAttribute("error", round + "회차 수집 실패: " + e.getMessage());
        }
        return "redirect:/admin/ops/collection";
    }

    @PostMapping("/stores/manual")
    public String saveManualStores(@RequestParam int round,
                                   @RequestParam int grade,
                                   @RequestParam String storesText,
                                   Principal principal,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String actor = extractActor(principal);
        String ip = ClientIpResolver.resolve(request, java.util.List.of());
        String ua = request.getHeader("User-Agent");
        try {
            List<WinningStore> stores = parseStoresText(round, grade, storesText);
            if (stores.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "입력된 판매점 데이터가 없습니다.");
                return "redirect:/admin/ops/collection";
            }
            winningStoreCollector.saveManual(round, grade, stores);
            auditLogService.recordSuccess(actor, "COLLECT_STORES_MANUAL",
                    "round:" + round + ",grade:" + grade + ",count:" + stores.size(), ip, ua);
            redirectAttributes.addFlashAttribute("message",
                    round + "회차 " + grade + "등 판매점 " + stores.size() + "건 수동 저장 완료");
        } catch (Exception e) {
            auditLogService.recordFailure(actor, "COLLECT_STORES_MANUAL",
                    "round:" + round + ",grade:" + grade, ip, ua, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "수동 저장 실패: " + e.getMessage());
        }
        return "redirect:/admin/ops/collection";
    }

    static List<WinningStore> parseStoresText(int round, int grade, String text) {
        return Arrays.stream(text.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> {
                    String[] parts = line.split("\\|", 3);
                    String name = parts[0].trim();
                    String address = parts.length > 1 ? parts[1].trim() : "";
                    int winCount = 1;
                    if (parts.length > 2) {
                        try { winCount = Integer.parseInt(parts[2].trim()); } catch (NumberFormatException ignored) {}
                    }
                    return WinningStore.of(round, grade, name, address, winCount);
                })
                .filter(s -> !s.name().isBlank())
                .toList();
    }

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}
