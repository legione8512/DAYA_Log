package ro.daya.dayalog.controller;

import java.util.List;
import java.util.UUID;

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

import jakarta.validation.Valid;
import ro.daya.dayalog.dto.client.AdminResetClientPasswordRequest;
import ro.daya.dayalog.dto.client.ClientAppointmentsTimelineResponse;
import ro.daya.dayalog.dto.client.ClientDetailsResponse;
import ro.daya.dayalog.dto.client.ClientSearchResponseItem;
import ro.daya.dayalog.dto.client.ClientStatusRequest;
import ro.daya.dayalog.dto.client.CreateClientRequest;
import ro.daya.dayalog.dto.client.CreateClientUserAccountRequest;
import ro.daya.dayalog.dto.client.UpdateClientRequest;
import ro.daya.dayalog.dto.common.MessageResponse;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.service.ClientService;

@RestController
@RequestMapping("/api/admin/clients")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping
	public List<ClientSearchResponseItem> search(@AuthenticationPrincipal CurrentUserPrincipal principal,
			@RequestParam(required = false) String query, @RequestParam(required = false) Boolean active) {
		return clientService.search(principal, query, active);
	}

	@GetMapping("/{id}")
	public ClientDetailsResponse getById(@AuthenticationPrincipal CurrentUserPrincipal principal,
			@PathVariable UUID id) {
		return clientService.getById(principal, id);
	}

	@GetMapping("/{id}/appointments")
	public ClientAppointmentsTimelineResponse getAppointmentsTimeline(
			@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID id) {
		return clientService.getAppointmentsTimeline(principal, id);
	}

	@PostMapping
	public ClientDetailsResponse create(@AuthenticationPrincipal CurrentUserPrincipal principal,
			@Valid @RequestBody CreateClientRequest request) {
		return clientService.create(principal, request);
	}

	@PutMapping("/{id}")
	public ClientDetailsResponse update(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody UpdateClientRequest request) {
		return clientService.update(principal, id, request);
	}

	@PatchMapping("/{id}/status")
	public void updateStatus(@AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody ClientStatusRequest request) {
		clientService.updateStatus(principal, id, request);
	}

	@PostMapping("/{id}/create-user-account")
	public ClientDetailsResponse createUserAccount(@AuthenticationPrincipal CurrentUserPrincipal principal,
			@PathVariable UUID id, @Valid @RequestBody CreateClientUserAccountRequest request) {
		return clientService.createUserAccount(principal, id, request);
	}

	@PostMapping("/{id}/reset-password")
	public MessageResponse resetClientPassword(@AuthenticationPrincipal CurrentUserPrincipal principal,
			@PathVariable UUID id, @Valid @RequestBody AdminResetClientPasswordRequest request) {
		return clientService.resetClientPassword(principal, id, request);
	}

}