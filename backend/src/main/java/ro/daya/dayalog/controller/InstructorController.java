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
import ro.daya.dayalog.dto.instructor.InstructorRequest;
import ro.daya.dayalog.dto.instructor.InstructorResponse;
import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursRequest;
import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.InstructorManagementService;
import ro.daya.dayalog.service.InstructorWorkingHoursService;

@RestController
@RequestMapping("/api/admin/instructors")
public class InstructorController {

    private final InstructorManagementService instructorManagementService;
    private final InstructorWorkingHoursService instructorWorkingHoursService;

    public InstructorController(InstructorManagementService instructorManagementService,
                                InstructorWorkingHoursService instructorWorkingHoursService) {
        this.instructorManagementService = instructorManagementService;
        this.instructorWorkingHoursService = instructorWorkingHoursService;
    }

    @GetMapping
    public List<InstructorResponse> list(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) Boolean active) {
        return instructorManagementService.list(principal, query, active);
    }

    @GetMapping("/{id}/working-hours")
    public InstructorWorkingHoursResponse getWorkingHours(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                          @PathVariable UUID id) {
        return instructorWorkingHoursService.getWorkingHours(principal, id);
    }

    @PutMapping("/{id}/working-hours")
    public InstructorWorkingHoursResponse replaceWorkingHours(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody InstructorWorkingHoursRequest request) {
        return instructorWorkingHoursService.replaceWorkingHours(principal, id, request);
    }

    @PostMapping
    public InstructorResponse create(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                     @Valid @RequestBody InstructorRequest request) {
        return instructorManagementService.create(principal, request);
    }

    @PutMapping("/{id}")
    public InstructorResponse update(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody InstructorRequest request) {
        return instructorManagementService.update(principal, id, request);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@AuthenticationPrincipal CurrentUserPrincipal principal,
                             @PathVariable UUID id,
                             @Valid @RequestBody StatusToggleRequest request) {
        instructorManagementService.updateStatus(principal, id, request);
    }
}