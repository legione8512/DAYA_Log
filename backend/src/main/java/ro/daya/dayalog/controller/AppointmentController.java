package ro.daya.dayalog.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.appointment.AddAppointmentParticipantsRequest;
import ro.daya.dayalog.dto.appointment.AddWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.AppointmentDetailsResponse;
import ro.daya.dayalog.dto.appointment.AppointmentFormOptionsResponse;
import ro.daya.dayalog.dto.appointment.AppointmentListFilterRequest;
import ro.daya.dayalog.dto.appointment.AppointmentListResponseItem;
import ro.daya.dayalog.dto.appointment.CancelAppointmentRequest;
import ro.daya.dayalog.dto.appointment.ChangeAppointmentStatusRequest;
import ro.daya.dayalog.dto.appointment.CreateAppointmentRequest;
import ro.daya.dayalog.dto.appointment.PromoteWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.PromoteWaitlistEntryResponse;
import ro.daya.dayalog.dto.appointment.RemoveAppointmentParticipantRequest;
import ro.daya.dayalog.dto.appointment.RemoveWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.UpdateAppointmentRequest;
import ro.daya.dayalog.dto.appointment.WaitlistEntryResponse;
import ro.daya.dayalog.dto.common.MessageResponse;
import ro.daya.dayalog.dto.common.PagedResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.AppointmentFormOptionsService;
import ro.daya.dayalog.service.AppointmentService;
import ro.daya.dayalog.service.AppointmentWaitlistService;

@RestController
@RequestMapping("/api/admin/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentFormOptionsService appointmentFormOptionsService;
    private final AppointmentWaitlistService appointmentWaitlistService;

    public AppointmentController(AppointmentService appointmentService,
                                 AppointmentFormOptionsService appointmentFormOptionsService,
                                 AppointmentWaitlistService appointmentWaitlistService) {
        this.appointmentService = appointmentService;
        this.appointmentFormOptionsService = appointmentFormOptionsService;
        this.appointmentWaitlistService = appointmentWaitlistService;
    }

    @GetMapping
    public PagedResponse<AppointmentListResponseItem> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @ModelAttribute AppointmentListFilterRequest request) {
        return appointmentService.list(principal, request);
    }

    @GetMapping("/form-options")
    public AppointmentFormOptionsResponse getFormOptions(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return appointmentFormOptionsService.getFormOptions(principal);
    }

    @GetMapping("/{id}")
    public AppointmentDetailsResponse getById(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                              @PathVariable UUID id) {
        return appointmentService.getById(principal, id);
    }

    @GetMapping("/{id}/waitlist")
    public List<WaitlistEntryResponse> getWaitlist(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                   @PathVariable UUID id) {
        return appointmentWaitlistService.list(principal, id);
    }

    @PostMapping
    public AppointmentDetailsResponse create(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                             @Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.create(principal, request);
    }

    @PutMapping("/{id}")
    public AppointmentDetailsResponse update(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                             @PathVariable UUID id,
                                             @Valid @RequestBody UpdateAppointmentRequest request) {
        return appointmentService.update(principal, id, request);
    }

    @PostMapping("/{id}/change-status")
    public AppointmentDetailsResponse changeStatus(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody ChangeAppointmentStatusRequest request) {
        return appointmentService.changeStatus(principal, id, request);
    }

    @PostMapping("/{id}/add-participants")
    public AppointmentDetailsResponse addParticipants(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody AddAppointmentParticipantsRequest request) {
        return appointmentService.addParticipants(principal, id, request);
    }

    @PostMapping("/{id}/remove-participant")
    public AppointmentDetailsResponse removeParticipant(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody RemoveAppointmentParticipantRequest request) {
        return appointmentService.removeParticipant(principal, id, request);
    }

    @PostMapping("/{id}/waitlist")
    public List<WaitlistEntryResponse> addWaitlistEntry(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody AddWaitlistEntryRequest request) {
        return appointmentWaitlistService.add(principal, id, request);
    }

    @PostMapping("/{id}/waitlist/remove")
    public List<WaitlistEntryResponse> removeWaitlistEntry(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                           @PathVariable UUID id,
                                                           @Valid @RequestBody RemoveWaitlistEntryRequest request) {
        return appointmentWaitlistService.remove(principal, id, request);
    }

    @PostMapping("/{id}/waitlist/promote")
    public PromoteWaitlistEntryResponse promoteWaitlistEntry(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody PromoteWaitlistEntryRequest request) {
        return appointmentWaitlistService.promote(principal, id, request);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@AuthenticationPrincipal CurrentUserPrincipal principal,
                       @PathVariable UUID id,
                       @RequestBody(required = false) CancelAppointmentRequest request) {
        appointmentService.cancel(principal, id, request);
    }

    @PostMapping("/{id}/send-confirmation")
    public MessageResponse sendConfirmation(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                            @PathVariable UUID id) {
        return appointmentService.sendConfirmation(principal, id);
    }
}