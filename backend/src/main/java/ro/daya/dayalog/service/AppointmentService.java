package ro.daya.dayalog.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.appointment.AppointmentDetailsResponse;
import ro.daya.dayalog.dto.appointment.AppointmentListFilterRequest;
import ro.daya.dayalog.dto.appointment.AppointmentListResponseItem;
import ro.daya.dayalog.dto.appointment.AppointmentParticipantResponse;
import ro.daya.dayalog.dto.appointment.CancelAppointmentRequest;
import ro.daya.dayalog.dto.appointment.CreateAppointmentRequest;
import ro.daya.dayalog.dto.appointment.UpdateAppointmentRequest;
import ro.daya.dayalog.dto.common.MessageResponse;
import ro.daya.dayalog.dto.common.PagedResponse;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.AppointmentParticipant;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;
import ro.daya.dayalog.entity.enums.ParticipantStatus;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.dto.appointment.AddAppointmentParticipantsRequest;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.dto.appointment.RemoveAppointmentParticipantRequest;
import ro.daya.dayalog.repository.AppointmentParticipantRepository;
import ro.daya.dayalog.dto.appointment.ChangeAppointmentStatusRequest;
import ro.daya.dayalog.exception.BadRequestException;

@Service
public class AppointmentService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DEFAULT_STUDIO_ZONE = ZoneId.of("Europe/Bucharest");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRuleService appointmentRuleService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final ClientRepository clientRepository;
    private final AppointmentParticipantRepository appointmentParticipantRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentRuleService appointmentRuleService,
                              EmailService emailService,
                              AuditLogService auditLogService, ClientRepository clientRepository, AppointmentParticipantRepository appointmentParticipantRepository) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentRuleService = appointmentRuleService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.clientRepository= clientRepository;
        this.appointmentParticipantRepository = appointmentParticipantRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AppointmentListResponseItem> list(CurrentUserPrincipal principal,
                                                           AppointmentListFilterRequest request) {
        validateListFilterRequest(request);

        AppointmentStatus status = parseAppointmentStatus(request.getStatus());
        AppointmentType appointmentType = parseAppointmentType(request.getAppointmentType());

        OffsetDateTime fromAt = request.getDateFrom() == null ? null : startOfDay(request.getDateFrom());
        OffsetDateTime toAt = request.getDateTo() == null ? null : startOfNextDay(request.getDateTo());

        PageRequest pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "startAt")
        );

        Page<Appointment> page = appointmentRepository.searchForList(
                principal.getStudioId(),
                fromAt,
                toAt,
                status,
                appointmentType,
                request.getServiceId(),
                request.getInstructorId(),
                request.getClientId(),
                pageable
        );

        List<AppointmentListResponseItem> content = page.getContent()
                .stream()
                .map(this::toListItem)
                .toList();

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AppointmentDetailsResponse getById(CurrentUserPrincipal principal, UUID id) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        return toDetailsResponse(appointment);
    }

    @Transactional
    public AppointmentDetailsResponse create(CurrentUserPrincipal principal, CreateAppointmentRequest request) {
        String initialStatus = normalizeCreateStatus(request.status());

        AppointmentRuleService.ResolvedAppointmentData resolved = appointmentRuleService.validateAndResolve(
                principal,
                request.appointmentType(),
                request.serviceId(),
                request.instructorId(),
                request.resourceId(),
                request.startAt(),
                request.endAt(),
                initialStatus,
                request.participantClientIds(),
                request.capacity(),
                null
        );

        Appointment appointment = new Appointment();
        appointment.setStudio(studioRef(principal.getStudioId()));
        appointment.setAppointmentType(resolved.appointmentType());
        appointment.setService(resolved.service());
        appointment.setInstructor(resolved.instructor());
        appointment.setResource(resolved.resource());
        appointment.setStartAt(resolved.startAt());
        appointment.setEndAt(resolved.endAt());
        appointment.setStatus(resolved.status());
        appointment.setCapacity(resolved.capacity());
        appointment.setNotes(blankToNull(request.notes()));
        appointment.setCreatedBy(userRef(principal.getId()));
        appointment.setUpdatedBy(userRef(principal.getId()));

        syncParticipants(appointment, resolved.participants());

        Appointment saved = appointmentRepository.save(appointment);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                saved.getId(),
                "CREATE",
                snapshotForAudit(saved)
        );

        return toDetailsResponse(saved);
    }

    @Transactional
    public AppointmentDetailsResponse update(CurrentUserPrincipal principal, UUID id, UpdateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        if (appointment.isCancelled()) {
            throw new ConflictException("APPOINTMENT_ALREADY_CANCELLED", "Programarea anulată nu mai poate fi modificată.");
        }

        validateStatusNotChangedInGenericUpdate(appointment, request.status());

        Map<String, Object> before = snapshotForAudit(appointment);

        AppointmentRuleService.ResolvedAppointmentData resolved = appointmentRuleService.validateAndResolve(
                principal,
                request.appointmentType(),
                request.serviceId(),
                request.instructorId(),
                request.resourceId(),
                request.startAt(),
                request.endAt(),
                appointment.getStatus().name(),
                request.participantClientIds(),
                request.capacity(),
                id
        );

        appointment.setAppointmentType(resolved.appointmentType());
        appointment.setService(resolved.service());
        appointment.setInstructor(resolved.instructor());
        appointment.setResource(resolved.resource());
        appointment.setStartAt(resolved.startAt());
        appointment.setEndAt(resolved.endAt());
        appointment.setStatus(resolved.status());
        appointment.setCapacity(resolved.capacity());
        appointment.setNotes(blankToNull(request.notes()));
        appointment.setUpdatedBy(userRef(principal.getId()));

        syncParticipants(appointment, resolved.participants());

        Map<String, Object> after = snapshotForAudit(appointment);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "UPDATE",
                beforeAfterSummary(before, after)
        );

        return toDetailsResponse(appointment);
    }

    @Transactional(readOnly = true)
    public MessageResponse sendConfirmation(CurrentUserPrincipal principal, UUID id) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        if (appointment.isCancelled()) {
            throw new ConflictException(
                    "APPOINTMENT_ALREADY_CANCELLED",
                    "Nu poți trimite confirmare pentru o programare anulată."
            );
        }

        if (appointment.getParticipants() == null || appointment.getParticipants().isEmpty()) {
            throw new ConflictException(
                    "APPOINTMENT_NO_PARTICIPANTS",
                    "Programarea nu are participanți."
            );
        }

        int sentCount = 0;

        for (AppointmentParticipant participant : appointment.getParticipants()) {
            Client client = participant.getClient();

            if (client.getEmail() == null || client.getEmail().isBlank()) {
                continue;
            }

            String subject = "Confirmare programare DAYA Log";
            String body = buildConfirmationEmailBody(client, appointment);

            emailService.send(client.getEmail(), subject, body);
            sentCount++;
        }

        if (sentCount == 0) {
            throw new ConflictException(
                    "APPOINTMENT_CONFIRMATION_NO_VALID_EMAILS",
                    "Niciun participant nu are adresă de email validă."
            );
        }

        return new MessageResponse("Emailul de confirmare a fost trimis către " + sentCount + " participant(i).");
    }

    @Transactional
    public void cancel(CurrentUserPrincipal principal, UUID id, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        appointmentRuleService.validateCancellation(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(OffsetDateTime.now());
        appointment.setCancelledBy(userRef(principal.getId()));
        appointment.setUpdatedBy(userRef(principal.getId()));

        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            appointment.setNotes(request.reason().trim());
        }

        for (AppointmentParticipant participant : appointment.getParticipants()) {
            participant.setParticipationStatus(ParticipantStatus.CANCELLED);
        }
    }
    
    @Transactional
    public AppointmentDetailsResponse addParticipants(CurrentUserPrincipal principal,
                                                      UUID id,
                                                      AddAppointmentParticipantsRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        validateCanAddParticipants(appointment);

        Map<String, Object> before = participantAuditSummary(appointment);

        List<Client> clientsToAdd = resolveClientsForAddition(
                principal.getStudioId(),
                appointment,
                request.participantClientIds()
        );

        ensureCapacityAllowsAdditionalParticipants(appointment, clientsToAdd);

        for (Client client : clientsToAdd) {
            AppointmentParticipant participant = new AppointmentParticipant();
            participant.setAppointment(appointment);
            participant.setClient(client);
            participant.setParticipationStatus(ParticipantStatus.BOOKED);
            appointment.getParticipants().add(participant);
        }

        appointment.setUpdatedBy(userRef(principal.getId()));

        Map<String, Object> after = participantAuditSummary(appointment);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "ADD_APPOINTMENT_PARTICIPANTS",
                beforeAfterSummary(before, after)
        );

        return toDetailsResponse(appointment);
    }
    
    @Transactional
    public AppointmentDetailsResponse removeParticipant(CurrentUserPrincipal principal,
                                                        UUID id,
                                                        RemoveAppointmentParticipantRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        validateCanRemoveParticipant(appointment);

        AppointmentParticipant participant = appointmentParticipantRepository
                .findByAppointmentIdAndClientId(id, request.clientId())
                .orElseThrow(() -> new NotFoundException(
                        "APPOINTMENT_PARTICIPANT_NOT_FOUND",
                        "Participantul nu a fost găsit în această programare."
                ));

        if (appointment.getParticipants().size() <= 1) {
            throw new ConflictException(
                    "APPOINTMENT_LAST_PARTICIPANT_REMOVE_FORBIDDEN",
                    "Nu poți elimina ultimul participant din programare. Anulează programarea dacă sesiunea nu mai are loc."
            );
        }

        Map<String, Object> before = participantAuditSummary(appointment);

        appointment.getParticipants().removeIf(existing -> existing.getId().equals(participant.getId()));
        appointmentParticipantRepository.delete(participant);
        appointment.setUpdatedBy(userRef(principal.getId()));

        Map<String, Object> after = participantAuditSummary(appointment);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "REMOVE_APPOINTMENT_PARTICIPANT",
                beforeAfterSummary(before, after)
        );

        return toDetailsResponse(appointment);
    }
    
    @Transactional
    public AppointmentDetailsResponse changeStatus(CurrentUserPrincipal principal,
                                                   UUID id,
                                                   ChangeAppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        AppointmentStatus targetStatus = parseTargetStatusForExplicitChange(request.status());

        validateExplicitStatusTransition(appointment, targetStatus);

        Map<String, Object> before = statusTransitionAuditSummary(appointment);

        appointment.setStatus(targetStatus);
        appointment.setUpdatedBy(userRef(principal.getId()));

        applyParticipantStatusesForAppointmentStatus(appointment, targetStatus);

        Map<String, Object> after = statusTransitionAuditSummary(appointment);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "CHANGE_APPOINTMENT_STATUS",
                beforeAfterSummary(before, after)
        );

        return toDetailsResponse(appointment);
    }
    
    private String normalizeCreateStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return AppointmentStatus.SCHEDULED.name();
        }

        AppointmentStatus parsedStatus;

        try {
            parsedStatus = AppointmentStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_CREATE_STATUS_INVALID_VALUE",
        	        "Status invalid pentru creare programare."
        	);        }

        if (parsedStatus != AppointmentStatus.SCHEDULED) {
            throw new ConflictException(
                    "APPOINTMENT_CREATE_STATUS_INVALID",
                    "Programarea nouă trebuie creată inițial cu status SCHEDULED."
            );
        }

        return parsedStatus.name();
    }

    private void validateStatusNotChangedInGenericUpdate(Appointment appointment, String requestedStatusValue) {
        if (requestedStatusValue == null || requestedStatusValue.isBlank()) {
            return;
        }

        AppointmentStatus requestedStatus;

        try {
            requestedStatus = AppointmentStatus.valueOf(requestedStatusValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_UPDATE_STATUS_INVALID_VALUE",
        	        "Status invalid pentru actualizare programare."
        	);        }

        if (requestedStatus != appointment.getStatus()) {
            throw new ConflictException(
                    "APPOINTMENT_USE_STATUS_CHANGE_ENDPOINT",
                    "Statusul programării trebuie modificat prin endpointul dedicat de schimbare status."
            );
        }
    }

    private AppointmentStatus parseTargetStatusForExplicitChange(String statusValue) {
        AppointmentStatus targetStatus;

        try {
            targetStatus = AppointmentStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_STATUS_CHANGE_INVALID_VALUE",
        	        "Status invalid pentru schimbarea explicită a programării."
        	);        }

        if (targetStatus == AppointmentStatus.CANCELLED) {
            throw new ConflictException(
                    "APPOINTMENT_USE_CANCEL_ENDPOINT",
                    "Pentru anulare folosește endpointul dedicat de cancel."
            );
        }

        if (targetStatus == AppointmentStatus.SCHEDULED) {
            throw new ConflictException(
                    "APPOINTMENT_STATUS_REVERT_FORBIDDEN",
                    "Revenirea la status SCHEDULED nu este permisă prin acest endpoint."
            );
        }

        return targetStatus;
    }

    private void validateExplicitStatusTransition(Appointment appointment, AppointmentStatus targetStatus) {
        AppointmentStatus currentStatus = appointment.getStatus();
        OffsetDateTime now = OffsetDateTime.now();

        if (currentStatus == targetStatus) {
            throw new ConflictException(
                    "APPOINTMENT_STATUS_UNCHANGED",
                    "Programarea are deja statusul cerut."
            );
        }

        if (currentStatus == AppointmentStatus.CANCELLED
                || currentStatus == AppointmentStatus.COMPLETED
                || currentStatus == AppointmentStatus.NO_SHOW) {
            throw new ConflictException(
                    "APPOINTMENT_STATUS_FINAL",
                    "Statusul programării nu mai poate fi schimbat din starea curentă."
            );
        }

        switch (targetStatus) {
            case CONFIRMED -> {
                if (currentStatus != AppointmentStatus.SCHEDULED) {
                    throw new ConflictException(
                            "APPOINTMENT_CONFIRM_INVALID_TRANSITION",
                            "Doar o programare SCHEDULED poate deveni CONFIRMED."
                    );
                }
            }
            case COMPLETED -> {
                if (currentStatus != AppointmentStatus.CONFIRMED) {
                    throw new ConflictException(
                            "APPOINTMENT_COMPLETE_INVALID_TRANSITION",
                            "Doar o programare CONFIRMED poate deveni COMPLETED."
                    );
                }

                if (now.isBefore(appointment.getEndAt())) {
                    throw new ConflictException(
                            "APPOINTMENT_COMPLETE_TOO_EARLY",
                            "Programarea poate fi marcată COMPLETED doar după ora de sfârșit."
                    );
                }
            }
            case NO_SHOW -> {
                if (currentStatus != AppointmentStatus.SCHEDULED
                        && currentStatus != AppointmentStatus.CONFIRMED) {
                    throw new ConflictException(
                            "APPOINTMENT_NO_SHOW_INVALID_TRANSITION",
                            "Doar o programare SCHEDULED sau CONFIRMED poate deveni NO_SHOW."
                    );
                }

                if (now.isBefore(appointment.getStartAt())) {
                    throw new ConflictException(
                            "APPOINTMENT_NO_SHOW_TOO_EARLY",
                            "Programarea poate fi marcată NO_SHOW doar după ora de început."
                    );
                }
            }
            default -> throw new BadRequestException(
                    "APPOINTMENT_STATUS_CHANGE_UNSUPPORTED",
                    "Statusul cerut nu este suportat de endpointul explicit."
            );        }
    }

    private void applyParticipantStatusesForAppointmentStatus(Appointment appointment, AppointmentStatus targetStatus) {
        if (targetStatus == AppointmentStatus.COMPLETED) {
            for (AppointmentParticipant participant : appointment.getParticipants()) {
                if (participant.getParticipationStatus() != ParticipantStatus.CANCELLED) {
                    participant.setParticipationStatus(ParticipantStatus.ATTENDED);
                }
            }
        }

        if (targetStatus == AppointmentStatus.NO_SHOW) {
            for (AppointmentParticipant participant : appointment.getParticipants()) {
                if (participant.getParticipationStatus() != ParticipantStatus.CANCELLED) {
                    participant.setParticipationStatus(ParticipantStatus.NO_SHOW);
                }
            }
        }
    }

    private Map<String, Object> statusTransitionAuditSummary(Appointment appointment) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", appointment.getStatus().name());
        summary.put("participantStatuses", appointment.getParticipants()
                .stream()
                .map(participant -> Map.of(
                        "clientId", participant.getClient().getId().toString(),
                        "participationStatus", participant.getParticipationStatus().name()
                ))
                .toList());
        summary.put("startAt", appointment.getStartAt().toString());
        summary.put("endAt", appointment.getEndAt().toString());
        return summary;
    }
    
    private void validateCanRemoveParticipant(Appointment appointment) {
        if (appointment.getAppointmentType() != AppointmentType.GROUP) {
            throw new ConflictException(
                    "APPOINTMENT_NOT_GROUP",
                    "Participantul poate fi eliminat doar dintr-o programare de grup."
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException(
                    "APPOINTMENT_ALREADY_CANCELLED",
                    "Nu poți modifica participanții unei programări anulate."
            );
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ConflictException(
                    "APPOINTMENT_NOT_OPEN_FOR_PARTICIPANT_REMOVAL",
                    "Nu poți elimina participanți dintr-o programare închisă."
            );
        }

        if (!appointment.getStartAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException(
                    "APPOINTMENT_ALREADY_STARTED",
                    "Nu poți elimina participanți după începerea programării."
            );
        }
    }
    
    private void validateCanAddParticipants(Appointment appointment) {
        if (appointment.getAppointmentType() != ro.daya.dayalog.entity.enums.AppointmentType.GROUP) {
            throw new ConflictException(
                    "APPOINTMENT_NOT_GROUP",
                    "Participanții suplimentari pot fi adăugați doar la programări de grup."
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException(
                    "APPOINTMENT_ALREADY_CANCELLED",
                    "Nu poți adăuga participanți la o programare anulată."
            );
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ConflictException(
                    "APPOINTMENT_NOT_OPEN_FOR_PARTICIPANTS",
                    "Nu poți adăuga participanți la o programare închisă."
            );
        }

        if (!appointment.getStartAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException(
                    "APPOINTMENT_ALREADY_STARTED",
                    "Nu poți adăuga participanți după începerea programării."
            );
        }
    }

    private List<Client> resolveClientsForAddition(UUID studioId,
                                                   Appointment appointment,
                                                   List<UUID> participantClientIds) {
        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(participantClientIds);

        if (uniqueIds.size() != participantClientIds.size()) {
        	throw new BadRequestException(
        	        "APPOINTMENT_PARTICIPANT_DUPLICATES",
        	        "Lista participanților conține duplicate."
        	);        }

        Set<UUID> existingClientIds = appointment.getParticipants()
                .stream()
                .map(participant -> participant.getClient().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<Client> clients = new java.util.ArrayList<>();

        for (UUID clientId : uniqueIds) {
            Client client = clientRepository.findByIdAndStudioId(clientId, studioId)
                    .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

            if (!Boolean.TRUE.equals(client.getActive())) {
                throw new ConflictException(
                        "CLIENT_INACTIVE",
                        "Clientul " + client.getFullName().trim() + " este inactiv."
                );
            }

            if (existingClientIds.contains(clientId)) {
                throw new ConflictException(
                        "APPOINTMENT_PARTICIPANT_ALREADY_EXISTS",
                        "Clientul " + client.getFullName().trim() + " este deja participant în această programare."
                );
            }

            if (!appointmentRepository.findClientOverlaps(
                    studioId,
                    client.getId(),
                    appointment.getStartAt(),
                    appointment.getEndAt(),
                    appointment.getId(),
                    AppointmentStatus.CANCELLED
            ).isEmpty()) {
                throw new ConflictException(
                        "APPOINTMENT_CLIENT_CONFLICT",
                        "Clientul " + client.getFullName().trim() + " are deja o programare în acest interval."
                );
            }

            clients.add(client);
        }

        return clients;
    }

    private void ensureCapacityAllowsAdditionalParticipants(Appointment appointment,
                                                            List<Client> clientsToAdd) {
        int currentParticipantCount = appointment.getParticipants().size();
        int requestedAdditionalCount = clientsToAdd.size();
        int totalAfterAdd = currentParticipantCount + requestedAdditionalCount;

        if (appointment.getCapacity() != null && totalAfterAdd > appointment.getCapacity()) {
            throw new ConflictException(
                    "APPOINTMENT_GROUP_CAPACITY_EXCEEDED",
                    "Capacitatea programării de grup ar fi depășită."
            );
        }
    }

    private Map<String, Object> participantAuditSummary(Appointment appointment) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("appointmentType", appointment.getAppointmentType().name());
        summary.put("status", appointment.getStatus().name());
        summary.put("capacity", appointment.getCapacity());
        summary.put(
                "participantClientIds",
                appointment.getParticipants()
                        .stream()
                        .map(participant -> participant.getClient().getId().toString())
                        .toList()
        );
        summary.put("participantCount", appointment.getParticipants().size());
        return summary;
    }

    private Map<String, Object> beforeAfterSummary(Map<String, Object> before,
                                                   Map<String, Object> after) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("before", before);
        summary.put("after", after);
        return summary;
    }

    private void validateListFilterRequest(AppointmentListFilterRequest request) {
        if (request.getDateFrom() != null
                && request.getDateTo() != null
                && request.getDateFrom().isAfter(request.getDateTo())) {
        	throw new BadRequestException(
        	        "APPOINTMENT_LIST_DATE_FILTER_INVALID",
        	        "Filtrul de dată este invalid. dateFrom nu poate fi după dateTo."
        	);        }
    }

    private AppointmentStatus parseAppointmentStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AppointmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_LIST_STATUS_INVALID",
        	        "Status invalid pentru filtrare."
        	);        }
    }

    private AppointmentType parseAppointmentType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AppointmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_LIST_TYPE_INVALID",
        	        "Tip de programare invalid pentru filtrare."
        	);        }
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(DEFAULT_STUDIO_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(DEFAULT_STUDIO_ZONE).toOffsetDateTime();
    }

    private void syncParticipants(Appointment appointment, List<Client> clients) {
        Set<UUID> desiredClientIds = clients.stream()
                .map(Client::getId)
                .collect(java.util.stream.Collectors.toSet());

        appointment.getParticipants().removeIf(existingParticipant ->
                !desiredClientIds.contains(existingParticipant.getClient().getId()));

        Set<UUID> existingClientIds = appointment.getParticipants().stream()
                .map(existingParticipant -> existingParticipant.getClient().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (Client client : clients) {
            if (!existingClientIds.contains(client.getId())) {
                AppointmentParticipant participant = new AppointmentParticipant();
                participant.setAppointment(appointment);
                participant.setClient(client);
                participant.setParticipationStatus(ParticipantStatus.BOOKED);
                appointment.getParticipants().add(participant);
            }
        }
    }

    private AppointmentListResponseItem toListItem(Appointment appointment) {
        String timeRange = appointment.getStartAt().toLocalTime().format(TIME_FORMATTER)
                + " - "
                + appointment.getEndAt().toLocalTime().format(TIME_FORMATTER);

        return new AppointmentListResponseItem(
                appointment.getId(),
                appointment.getStartAt().toLocalDate(),
                timeRange,
                appointment.getAppointmentType().name(),
                appointment.getService().getName(),
                appointment.getInstructor().getFullName(),
                appointment.getParticipants().size(),
                appointment.getStatus().name()
        );
    }

    private AppointmentDetailsResponse toDetailsResponse(Appointment appointment) {
        List<AppointmentParticipantResponse> participants = appointment.getParticipants()
                .stream()
                .map(p -> new AppointmentParticipantResponse(
                        p.getClient().getId(),
                        p.getClient().getFullName().trim(),
                        p.getParticipationStatus().name()
                ))
                .toList();

        return new AppointmentDetailsResponse(
                appointment.getId(),
                appointment.getAppointmentType().name(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getInstructor().getId(),
                appointment.getInstructor().getFullName(),
                appointment.getResource() == null ? null : appointment.getResource().getId(),
                appointment.getResource() == null ? null : appointment.getResource().getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus().name(),
                appointment.getCapacity(),
                appointment.getNotes(),
                participants
        );
    }

    private Map<String, Object> snapshotForAudit(Appointment appointment) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("appointmentType", appointment.getAppointmentType().name());
        summary.put("serviceId", appointment.getService().getId().toString());
        summary.put("serviceName", appointment.getService().getName());
        summary.put("instructorId", appointment.getInstructor().getId().toString());
        summary.put("instructorName", appointment.getInstructor().getFullName());
        if (appointment.getResource() != null) {
            summary.put("resourceId", appointment.getResource().getId().toString());
            summary.put("resourceName", appointment.getResource().getName());
        }
        summary.put("startAt", appointment.getStartAt().toString());
        summary.put("endAt", appointment.getEndAt().toString());
        summary.put("status", appointment.getStatus().name());
        summary.put("capacity", appointment.getCapacity());
        summary.put("notes", appointment.getNotes());
        summary.put("participantClientIds", participantClientIdsAsStrings(appointment));
        summary.put("participantCount", appointment.getParticipants().size());
        return summary;
    }

    private List<String> participantClientIdsAsStrings(Appointment appointment) {
        return appointment.getParticipants()
                .stream()
                .map(participant -> participant.getClient().getId().toString())
                .toList();
    }

    private Studio studioRef(UUID studioId) {
        Studio studio = new Studio();
        studio.setId(studioId);
        return studio;
    }

    private AppUser userRef(UUID userId) {
        AppUser user = new AppUser();
        user.setId(userId);
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildConfirmationEmailBody(Client client, Appointment appointment) {
        String serviceName = appointment.getService().getName();
        String instructorName = appointment.getInstructor().getFullName();
        String resourceName = appointment.getResource() == null ? "Nespecificată" : appointment.getResource().getName();

        return """
                Bună %s,

                Programarea ta a fost confirmată.

                Detalii:
                - Serviciu: %s
                - Instructor: %s
                - Resursă: %s
                - Început: %s
                - Sfârșit: %s
                - Status: %s

                Te așteptăm cu drag!
                Echipa DAYA Log
                """.formatted(
                client.getFullName().trim(),
                serviceName,
                instructorName,
                resourceName,
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus().name()
        );
    }
}