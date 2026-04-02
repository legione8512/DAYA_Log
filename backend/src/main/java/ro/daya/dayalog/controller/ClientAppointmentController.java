package ro.daya.dayalog.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.appointment.ClientAppointmentResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ClientAppointmentService;

@RestController
@RequestMapping("/api/client/appointments")
public class ClientAppointmentController {

    private final ClientAppointmentService clientAppointmentService;

    public ClientAppointmentController(ClientAppointmentService clientAppointmentService) {
        this.clientAppointmentService = clientAppointmentService;
    }

    @GetMapping("/future")
    public List<ClientAppointmentResponse> future(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return clientAppointmentService.future(principal);
    }

    @GetMapping("/history")
    public List<ClientAppointmentResponse> history(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return clientAppointmentService.history(principal);
    }
}