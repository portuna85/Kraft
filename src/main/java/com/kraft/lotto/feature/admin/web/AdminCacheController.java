package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ops/cache")
public class AdminCacheController {

    static final List<String> CACHE_NAMES = List.of(
            "winningNumberFrequency", "combinationPrizeHistory", "winningFrequencySummary",
            "winningNumberFrequencyPeriod", "patternStats", "companionNumbers"
    );

    private final CacheManager cacheManager;
    private final AdminAuditLogService auditLogService;

    public AdminCacheController(CacheManager cacheManager, AdminAuditLogService auditLogService) {
        this.cacheManager = cacheManager;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String cachePage(Model model) {
        List<CacheStat> stats = CACHE_NAMES.stream().map(this::buildStat).toList();
        model.addAttribute("cacheStats", stats);
        return "admin/cache";
    }

    @PostMapping("/{name}/evict")
    public String evictOne(@PathVariable String name,
                           Principal principal,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        if (!CACHE_NAMES.contains(name)) {
            redirectAttributes.addFlashAttribute("error", "알 수 없는 캐시: " + name);
            return "redirect:/admin/ops/cache";
        }
        org.springframework.cache.Cache cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
        String actor = extractActor(principal);
        auditLogService.recordSuccess(actor, "CACHE_EVICT", "cache:" + name,
                ClientIpResolver.resolve(request, List.of()), request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", name + " 캐시 초기화 완료");
        return "redirect:/admin/ops/cache";
    }

    @PostMapping("/evict-all")
    public String evictAll(Principal principal,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        CACHE_NAMES.forEach(name -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        String actor = extractActor(principal);
        auditLogService.recordSuccess(actor, "CACHE_EVICT_ALL", "all",
                ClientIpResolver.resolve(request, List.of()), request.getHeader("User-Agent"));
        redirectAttributes.addFlashAttribute("message", "전체 캐시 초기화 완료");
        return "redirect:/admin/ops/cache";
    }

    private CacheStat buildStat(String name) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(name);
        if (springCache instanceof CaffeineCache caffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();
            long total = stats.hitCount() + stats.missCount();
            int hitRatePct = total > 0 ? (int) (stats.hitCount() * 100L / total) : 0;
            return new CacheStat(name, nativeCache.estimatedSize(),
                    stats.hitCount(), stats.missCount(), hitRatePct, stats.evictionCount());
        }
        return new CacheStat(name, -1L, -1L, -1L, -1, -1L);
    }

    private static String extractActor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }

    public record CacheStat(String name, long size, long hits, long misses, int hitRatePct, long evictions) {}
}
