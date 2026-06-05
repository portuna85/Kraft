package com.kraft.lotto.feature.admin.web;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
            Model model) {
        var pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        model.addAttribute("logs", auditLogService.list(pageable));
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        return "admin/audit";
    }
}
