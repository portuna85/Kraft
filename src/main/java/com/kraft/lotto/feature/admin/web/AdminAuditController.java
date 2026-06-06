package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminAuditLogService.AuditFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequestMapping("/admin/ops/audit")
public class AdminAuditController {

    private final AdminAuditLogService auditLogService;

    public AdminAuditController(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String auditLog(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(10) @Max(200) int pageSize,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        var pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        var filter = new AuditFilter(action, result, from, to);
        model.addAttribute("logs", auditLogService.list(filter, pageable));
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("filterAction", action != null ? action : "");
        model.addAttribute("filterResult", result != null ? result : "");
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        return "admin/audit";
    }
}
