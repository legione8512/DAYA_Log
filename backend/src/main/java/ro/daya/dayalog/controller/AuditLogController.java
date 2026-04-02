package ro.daya.dayalog.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.common.AuditLogDetailsResponse;
import ro.daya.dayalog.dto.common.AuditLogFilterRequest;
import ro.daya.dayalog.dto.common.AuditLogResponseItem;
import ro.daya.dayalog.dto.common.PagedResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.AuditLogService;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PagedResponse<AuditLogResponseItem> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @ModelAttribute AuditLogFilterRequest request) {
        return auditLogService.list(principal, request);
    }

    @GetMapping("/{id}")
    public AuditLogDetailsResponse getById(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                           @PathVariable UUID id) {
        return auditLogService.getById(principal, id);
    }
}