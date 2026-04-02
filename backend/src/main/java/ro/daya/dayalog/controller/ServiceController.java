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
import ro.daya.dayalog.dto.service.ServiceRequest;
import ro.daya.dayalog.dto.service.ServiceResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ServiceManagementService;

@RestController
@RequestMapping("/api/admin/services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @GetMapping
    public List<ServiceResponse> list(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                      @RequestParam(required = false) String query,
                                      @RequestParam(required = false) Boolean active) {
        return serviceManagementService.list(principal, query, active);
    }

    @PostMapping
    public ServiceResponse create(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                  @Valid @RequestBody ServiceRequest request) {
        return serviceManagementService.create(principal, request);
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody ServiceRequest request) {
        return serviceManagementService.update(principal, id, request);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@AuthenticationPrincipal CurrentUserPrincipal principal,
                             @PathVariable UUID id,
                             @Valid @RequestBody StatusToggleRequest request) {
        serviceManagementService.updateStatus(principal, id, request);
    }
}