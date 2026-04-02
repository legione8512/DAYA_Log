package ro.daya.dayalog.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.common.StatusToggleRequest;
import ro.daya.dayalog.dto.resource.ResourceRequest;
import ro.daya.dayalog.dto.resource.ResourceResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ResourceManagementService;

@RestController
@RequestMapping("/api/admin/resources")
public class ResourceController {

    private final ResourceManagementService resourceManagementService;

    public ResourceController(ResourceManagementService resourceManagementService) {
        this.resourceManagementService = resourceManagementService;
    }

    @GetMapping
    public List<ResourceResponse> list(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                       @RequestParam(required = false) String query,
                                       @RequestParam(required = false) Boolean active) {
        return resourceManagementService.list(principal, query, active);
    }

    @PostMapping
    public ResourceResponse create(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                   @Valid @RequestBody ResourceRequest request) {
        return resourceManagementService.create(principal, request);
    }

    @PutMapping("/{id}")
    public ResourceResponse update(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody ResourceRequest request) {
        return resourceManagementService.update(principal, id, request);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@AuthenticationPrincipal CurrentUserPrincipal principal,
                             @PathVariable UUID id,
                             @Valid @RequestBody StatusToggleRequest request) {
        resourceManagementService.updateStatus(principal, id, request);
    }
}