package ro.daya.dayalog.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.client.ClientAppointmentTimelineItemResponse;
import ro.daya.dayalog.dto.client.ClientAppointmentsTimelineResponse;
import ro.daya.dayalog.dto.client.ClientDetailsResponse;
import ro.daya.dayalog.dto.client.ClientRecentAppointmentResponse;
import ro.daya.dayalog.dto.client.ClientSearchResponseItem;
import ro.daya.dayalog.dto.client.ClientStatusRequest;
import ro.daya.dayalog.dto.client.CreateClientRequest;
import ro.daya.dayalog.dto.client.CreateClientUserAccountRequest;
import ro.daya.dayalog.dto.client.UpdateClientRequest;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.UserRole;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AppUserRepository;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.BadRequestException;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public ClientService(ClientRepository clientRepository,
                         AppUserRepository appUserRepository,
                         AppointmentRepository appointmentRepository,
                         PasswordEncoder passwordEncoder,
                         AuthService authService,
                         AuditLogService auditLogService) {
        this.clientRepository = clientRepository;
        this.appUserRepository = appUserRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ClientDetailsResponse createUserAccount(CurrentUserPrincipal principal,
                                                   UUID clientId,
                                                   CreateClientUserAccountRequest request) {
        Client client = clientRepository.findByIdAndStudioId(clientId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        if (client.getUser() != null) {
            throw new ConflictException("CLIENT_USER_ACCOUNT_ALREADY_EXISTS", "Clientul are deja un cont de utilizator.");
        }

        String normalizedEmail = normalizeEmail(request.email());

        if (normalizedEmail == null) {
        	throw new BadRequestException(
        	        "CLIENT_USER_ACCOUNT_EMAIL_REQUIRED",
        	        "Emailul este obligatoriu."
        	);        }

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("USER_EMAIL_CONFLICT", "Există deja un cont cu acest email.");
        }

        AppUser user = new AppUser();
        user.setStudio(studioRef(principal.getStudioId()));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        user.setRole(UserRole.CLIENT);
        user.setEmailVerified(false);
        user.setForcePasswordChange(Boolean.TRUE.equals(request.forcePasswordChange()));
        user.setActive(true);

        AppUser savedUser = appUserRepository.save(user);

        client.setUser(savedUser);
        client.setEmail(normalizedEmail);

        authService.sendEmailConfirmation(savedUser);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("clientId", client.getId().toString());
        summary.put("clientName", client.getFullName().trim());
        summary.put("accountEmail", savedUser.getEmail());
        summary.put("accountRole", savedUser.getRole().name());
        summary.put("forcePasswordChange", savedUser.getForcePasswordChange());
        summary.put("emailConfirmationTriggered", true);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "client",
                client.getId(),
                "CREATE_CLIENT_USER_ACCOUNT",
                summary
        );

        return toDetailsResponse(client);
    }

    @Transactional(readOnly = true)
    public List<ClientSearchResponseItem> search(CurrentUserPrincipal principal,
                                                 String query,
                                                 Boolean active) {
        UUID studioId = principal.getStudioId();
        String normalizedQuery = normalizeQuery(query);

        List<Client> clients;

        if (normalizedQuery == null) {
            clients = (active == null)
                    ? clientRepository.findTop20ByStudioIdOrderByLastNameAscFirstNameAsc(studioId)
                    : clientRepository.findTop20ByStudioIdAndActiveOrderByLastNameAscFirstNameAsc(studioId, active);
        } else {
            clients = clientRepository.searchByStudioIdAndFilters(studioId, normalizedQuery, active);
        }

        return clients.stream()
                .map(this::toSearchItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDetailsResponse getById(CurrentUserPrincipal principal, UUID clientId) {
        Client client = clientRepository.findByIdAndStudioId(clientId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        return toDetailsResponse(client);
    }

    @Transactional
    public ClientDetailsResponse create(CurrentUserPrincipal principal, CreateClientRequest request) {
        validateCreate(principal.getStudioId(), request);

        Client client = new Client();
        client.setStudio(studioRef(principal.getStudioId()));
        applyEditableFields(client, request);
        client.setActive(true);

        Client saved = clientRepository.save(client);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "client",
                saved.getId(),
                "CREATE_CLIENT",
                clientAuditSummary(saved)
        );

        return toDetailsResponse(saved);
    }

    @Transactional
    public ClientDetailsResponse update(CurrentUserPrincipal principal, UUID clientId, UpdateClientRequest request) {
        Client client = clientRepository.findByIdAndStudioId(clientId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        Map<String, Object> before = clientAuditSummary(client);

        validateUpdate(principal.getStudioId(), client, request);

        applyEditableFields(client, request);
        syncLinkedUserEmail(client);

        Map<String, Object> after = clientAuditSummary(client);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "client",
                client.getId(),
                "UPDATE_CLIENT",
                beforeAfterSummary(before, after)
        );

        return toDetailsResponse(client);
    }

    @Transactional
    public void updateStatus(CurrentUserPrincipal principal, UUID clientId, ClientStatusRequest request) {
        Client client = clientRepository.findByIdAndStudioId(clientId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        Boolean previousActive = client.getActive();
        client.setActive(request.active());

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("beforeActive", previousActive);
        summary.put("afterActive", client.getActive());
        summary.put("clientName", client.getFullName().trim());
        summary.put("hasUserAccount", client.hasUserAccount());

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "client",
                client.getId(),
                "UPDATE_CLIENT_STATUS",
                summary
        );
    }

    private void validateCreate(UUID studioId, CreateClientRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        if (normalizedEmail != null
                && clientRepository.existsByStudioIdAndEmailIgnoreCase(studioId, normalizedEmail)) {
            throw new ConflictException("CLIENT_EMAIL_CONFLICT", "Există deja un client cu acest email.");
        }

        if (normalizedPhone != null
                && clientRepository.existsByStudioIdAndPhone(studioId, normalizedPhone)) {
            throw new ConflictException("CLIENT_PHONE_CONFLICT", "Există deja un client cu acest telefon.");
        }
    }

    private void validateUpdate(UUID studioId, Client client, UpdateClientRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        if (normalizedEmail != null
                && clientRepository.existsByStudioIdAndEmailIgnoreCaseAndIdNot(studioId, normalizedEmail, client.getId())) {
            throw new ConflictException("CLIENT_EMAIL_CONFLICT", "Există deja un client cu acest email.");
        }

        if (normalizedPhone != null
                && clientRepository.existsByStudioIdAndPhoneAndIdNot(studioId, normalizedPhone, client.getId())) {
            throw new ConflictException("CLIENT_PHONE_CONFLICT", "Există deja un client cu acest telefon.");
        }

        if (client.getUser() != null) {
            if (normalizedEmail == null) {
                throw new ConflictException(
                        "CLIENT_LINKED_USER_EMAIL_REQUIRED",
                        "Clientul cu cont de utilizator trebuie să aibă un email."
                );
            }

            if (appUserRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, client.getUser().getId())) {
                throw new ConflictException("USER_EMAIL_CONFLICT", "Există deja un cont cu acest email.");
            }
        }
    }

    private void applyEditableFields(Client client, CreateClientRequest request) {
        client.setFirstName(request.firstName().trim());
        client.setLastName(request.lastName().trim());
        client.setEmail(normalizeEmail(request.email()));
        client.setPhone(normalizePhone(request.phone()));
        client.setAddressLine1(blankToNull(request.addressLine1()));
        client.setAddressLine2(blankToNull(request.addressLine2()));
        client.setCity(blankToNull(request.city()));
        client.setCounty(blankToNull(request.county()));
        client.setPostcode(blankToNull(request.postcode()));
        client.setDateOfBirth(request.dateOfBirth());
        client.setGender(parseGender(request.gender()));
        client.setLeadSource(blankToNull(request.leadSource()));
        client.setGdprConsent(Boolean.TRUE.equals(request.gdprConsent()));
        client.setEmailAllowed(Boolean.TRUE.equals(request.emailAllowed()));
        client.setSmsAllowed(Boolean.TRUE.equals(request.smsAllowed()));
        client.setMarketingAllowed(Boolean.TRUE.equals(request.marketingAllowed()));
        client.setEmergencyContactName(blankToNull(request.emergencyContactName()));
        client.setEmergencyContactPhone(blankToNull(request.emergencyContactPhone()));
        client.setMedicalNotes(blankToNull(request.medicalNotes()));
        client.setRestrictions(blankToNull(request.restrictions()));
    }

    private void applyEditableFields(Client client, UpdateClientRequest request) {
        client.setFirstName(request.firstName().trim());
        client.setLastName(request.lastName().trim());
        client.setEmail(normalizeEmail(request.email()));
        client.setPhone(normalizePhone(request.phone()));
        client.setAddressLine1(blankToNull(request.addressLine1()));
        client.setAddressLine2(blankToNull(request.addressLine2()));
        client.setCity(blankToNull(request.city()));
        client.setCounty(blankToNull(request.county()));
        client.setPostcode(blankToNull(request.postcode()));
        client.setDateOfBirth(request.dateOfBirth());
        client.setGender(parseGender(request.gender()));
        client.setLeadSource(blankToNull(request.leadSource()));
        client.setGdprConsent(Boolean.TRUE.equals(request.gdprConsent()));
        client.setEmailAllowed(Boolean.TRUE.equals(request.emailAllowed()));
        client.setSmsAllowed(Boolean.TRUE.equals(request.smsAllowed()));
        client.setMarketingAllowed(Boolean.TRUE.equals(request.marketingAllowed()));
        client.setEmergencyContactName(blankToNull(request.emergencyContactName()));
        client.setEmergencyContactPhone(blankToNull(request.emergencyContactPhone()));
        client.setMedicalNotes(blankToNull(request.medicalNotes()));
        client.setRestrictions(blankToNull(request.restrictions()));
    }

    private void syncLinkedUserEmail(Client client) {
        if (client.getUser() == null) {
            return;
        }

        String normalizedEmail = normalizeEmail(client.getEmail());

        if (normalizedEmail == null) {
            throw new ConflictException(
                    "CLIENT_LINKED_USER_EMAIL_REQUIRED",
                    "Clientul cu cont de utilizator trebuie să aibă un email."
            );
        }

        client.getUser().setEmail(normalizedEmail);
        client.setEmail(normalizedEmail);
    }

    private ClientSearchResponseItem toSearchItem(Client client) {
        return new ClientSearchResponseItem(
                client.getId(),
                client.getFullName().trim(),
                client.getEmail(),
                client.getPhone(),
                client.getActive(),
                client.hasUserAccount()
        );
    }

    private ClientDetailsResponse toDetailsResponse(Client client) {
        List<ClientRecentAppointmentResponse> recentAppointments = loadRecentAppointments(
                client.getStudio().getId(),
                client.getId()
        );

        return new ClientDetailsResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddressLine1(),
                client.getAddressLine2(),
                client.getCity(),
                client.getCounty(),
                client.getPostcode(),
                client.getDateOfBirth(),
                client.getGender() == null ? null : client.getGender().name(),
                client.getLeadSource(),
                client.getGdprConsent(),
                client.getEmailAllowed(),
                client.getSmsAllowed(),
                client.getMarketingAllowed(),
                client.getEmergencyContactName(),
                client.getEmergencyContactPhone(),
                client.getMedicalNotes(),
                client.getRestrictions(),
                client.getActive(),
                client.hasUserAccount(),
                client.getUser() == null ? null : client.getUser().getEmail(),
                client.getUser() == null ? null : client.getUser().getRole().name(),
                client.getUser() == null ? null : client.getUser().getForcePasswordChange(),
                recentAppointments
        );
    }

    private List<ClientRecentAppointmentResponse> loadRecentAppointments(UUID studioId, UUID clientId) {
        return appointmentRepository.findRecentHistoryForClientAdmin(
                        studioId,
                        clientId,
                        OffsetDateTime.now(),
                        AppointmentStatus.CANCELLED,
                        PageRequest.of(0, 5)
                )
                .stream()
                .map(this::toRecentAppointmentResponse)
                .toList();
    }

    private ClientRecentAppointmentResponse toRecentAppointmentResponse(Appointment appointment) {
        return new ClientRecentAppointmentResponse(
                appointment.getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getAppointmentType().name(),
                appointment.getStatus().name(),
                appointment.getService().getName(),
                appointment.getInstructor().getFullName(),
                appointment.getResource() == null ? null : appointment.getResource().getName()
        );
    }

    private Map<String, Object> clientAuditSummary(Client client) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("firstName", client.getFirstName());
        summary.put("lastName", client.getLastName());
        summary.put("email", client.getEmail());
        summary.put("phone", client.getPhone());
        summary.put("active", client.getActive());
        summary.put("hasUserAccount", client.hasUserAccount());

        if (client.getUser() != null) {
            summary.put("userId", client.getUser().getId().toString());
            summary.put("accountEmail", client.getUser().getEmail());
            summary.put("accountRole", client.getUser().getRole().name());
        }

        return summary;
    }

    private Map<String, Object> beforeAfterSummary(Map<String, Object> before,
                                                   Map<String, Object> after) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("before", before);
        summary.put("after", after);
        return summary;
    }
    @Transactional(readOnly = true)
    public ClientAppointmentsTimelineResponse getAppointmentsTimeline(CurrentUserPrincipal principal, UUID clientId) {
        Client client = clientRepository.findByIdAndStudioId(clientId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        OffsetDateTime now = OffsetDateTime.now();

        List<ClientAppointmentTimelineItemResponse> futureAppointments = appointmentRepository
                .findFutureForClientAdmin(
                        principal.getStudioId(),
                        clientId,
                        now,
                        AppointmentStatus.CANCELLED
                )
                .stream()
                .map(this::toTimelineItem)
                .toList();

        List<ClientAppointmentTimelineItemResponse> historyAppointments = appointmentRepository
                .findHistoryForClientAdmin(
                        principal.getStudioId(),
                        clientId,
                        now,
                        AppointmentStatus.CANCELLED
                )
                .stream()
                .map(this::toTimelineItem)
                .toList();

        return new ClientAppointmentsTimelineResponse(
                client.getId(),
                client.getFullName().trim(),
                futureAppointments,
                historyAppointments
        );
    }

    private ClientAppointmentTimelineItemResponse toTimelineItem(Appointment appointment) {
        return new ClientAppointmentTimelineItemResponse(
                appointment.getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getAppointmentType().name(),
                appointment.getStatus().name(),
                appointment.getService().getName(),
                appointment.getInstructor().getFullName(),
                appointment.getResource() == null ? null : appointment.getResource().getName()
        );
    }

    private Studio studioRef(UUID studioId) {
        Studio studio = new Studio();
        studio.setId(studioId);
        return studio;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ro.daya.dayalog.entity.enums.GenderType parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return ro.daya.dayalog.entity.enums.GenderType.valueOf(value.trim().toUpperCase());
    }
}