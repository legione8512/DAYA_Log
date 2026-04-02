package ro.daya.dayalog.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.client.ClientDashboardResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ClientDashboardService;

@RestController
@RequestMapping("/api/client/dashboard")
public class ClientDashboardController {

    private final ClientDashboardService clientDashboardService;

    public ClientDashboardController(ClientDashboardService clientDashboardService) {
        this.clientDashboardService = clientDashboardService;
    }

    @GetMapping
    public ClientDashboardResponse getDashboard(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return clientDashboardService.getDashboard(principal);
    }
}