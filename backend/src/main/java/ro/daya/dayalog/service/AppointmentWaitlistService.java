package ro.daya.dayalog.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.appointment.AddWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.AppointmentDetailsResponse;
import ro.daya.dayalog.dto.appointment.AppointmentParticipantResponse;
import ro.daya.dayalog.dto.appointment.PromoteWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.PromoteWaitlistEntryResponse;
import ro.daya.dayalog.dto.appointment.RemoveWaitlistEntryRequest;
import ro.daya.dayalog.dto.appointment.WaitlistEntryResponse;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.AppointmentParticipant;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.WaitlistEntry;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.ParticipantStatus;
import ro.daya.dayalog.entity.enums.WaitlistStatus;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.repository.WaitlistEntryRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class AppointmentWaitlistService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final AuditLogService auditLogService;

    public AppointmentWaitlistService(AppointmentRepository appointmentRepository,
                                      ClientRepository clientRepository,
                                      WaitlistEntryRepository waitlistEntryRepository,
                                      AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> list(CurrentUserPrincipal principal, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(appointmentId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        return waitlistEntryRepository
                .findByAppointmentIdAndStudioIdAndStatusOrderByPositionAsc(
                        appointment.getId(),
                        principal.getStudioId(),
                        WaitlistStatus.ACTIVE
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WaitlistEntryResponse> add(CurrentUserPrincipal principal,
                                           UUID appointmentId,
                                           AddWaitlistEntryRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(appointmentId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        validateCanModifyWaitlist(appointment);
        validateAppointmentIsFull(appointment);

        Client client = clientRepository.findByIdAndStudioId(request.clientId(), principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

        if (!Boolean.TRUE.equals(client.getActive())) {
            throw new ConflictException("CLIENT_INACTIVE", "Clientul selectat este inactiv.");
        }

        boolean alreadyParticipant = appointment.getParticipants()
                .stream()
                .anyMatch(participant -> participant.getClient().getId().equals(client.getId()));

        if (alreadyParticipant) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_CLIENT_ALREADY_PARTICIPANT",
                    "Clientul este deja participant în această programare."
            );
        }

        if (waitlistEntryRepository.existsByAppointmentIdAndClientIdAndStatus(
                appointment.getId(),
                client.getId(),
                WaitlistStatus.ACTIVE
        )) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_ENTRY_ALREADY_EXISTS",
                    "Clientul este deja în lista de așteptare pentru această programare."
            );
        }

        Integer maxPosition = waitlistEntryRepository.findMaxPositionByAppointmentIdAndStatus(
                appointment.getId(),
                WaitlistStatus.ACTIVE
        );

        WaitlistEntry entry = new WaitlistEntry();
        entry.setStudio(studioRef(principal.getStudioId()));
        entry.setAppointment(appointment);
        entry.setClient(client);
        entry.setStatus(WaitlistStatus.ACTIVE);
        entry.setPosition((maxPosition == null ? 0 : maxPosition) + 1);

        waitlistEntryRepository.save(entry);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "ADD_APPOINTMENT_WAITLIST_ENTRY",
                addAuditSummary(entry)
        );

        return list(principal, appointmentId);
    }

    @Transactional
    public List<WaitlistEntryResponse> remove(CurrentUserPrincipal principal,
                                              UUID appointmentId,
                                              RemoveWaitlistEntryRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(appointmentId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        validateCanModifyWaitlist(appointment);

        WaitlistEntry entry = waitlistEntryRepository.findByIdAndAppointmentIdAndStudioIdAndStatus(
                        request.waitlistEntryId(),
                        appointmentId,
                        principal.getStudioId(),
                        WaitlistStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException(
                        "WAITLIST_ENTRY_NOT_FOUND",
                        "Intrarea din lista de așteptare nu a fost găsită."
                ));

        entry.setStatus(WaitlistStatus.REMOVED);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "REMOVE_APPOINTMENT_WAITLIST_ENTRY",
                removeAuditSummary(entry)
        );

        renumberActiveEntries(appointmentId, principal.getStudioId());

        return list(principal, appointmentId);
    }

    @Transactional
    public PromoteWaitlistEntryResponse promote(CurrentUserPrincipal principal,
                                                UUID appointmentId,
                                                PromoteWaitlistEntryRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndStudioId(appointmentId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("APPOINTMENT_NOT_FOUND", "Programarea nu a fost găsită."));

        validateCanModifyWaitlist(appointment);

        WaitlistEntry entry = waitlistEntryRepository.findByIdAndAppointmentIdAndStudioIdAndStatus(
                        request.waitlistEntryId(),
                        appointmentId,
                        principal.getStudioId(),
                        WaitlistStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException(
                        "WAITLIST_ENTRY_NOT_FOUND",
                        "Intrarea din lista de așteptare nu a fost găsită."
                ));

        ensureCapacityHasFreeSlotForPromotion(appointment);

        Client client = entry.getClient();

        if (!Boolean.TRUE.equals(client.getActive())) {
            throw new ConflictException("CLIENT_INACTIVE", "Clientul selectat este inactiv.");
        }

        boolean alreadyParticipant = appointment.getParticipants()
                .stream()
                .anyMatch(participant -> participant.getClient().getId().equals(client.getId()));

        if (alreadyParticipant) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_CLIENT_ALREADY_PARTICIPANT",
                    "Clientul este deja participant în această programare."
            );
        }

        if (!appointmentRepository.findClientOverlaps(
                principal.getStudioId(),
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

        AppointmentParticipant participant = new AppointmentParticipant();
        participant.setAppointment(appointment);
        participant.setClient(client);
        participant.setParticipationStatus(ParticipantStatus.BOOKED);
        appointment.getParticipants().add(participant);
        appointment.setUpdatedBy(userRef(principal.getId()));

        entry.setStatus(WaitlistStatus.PROMOTED);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "appointment",
                appointment.getId(),
                "PROMOTE_APPOINTMENT_WAITLIST_ENTRY",
                promoteAuditSummary(entry, appointment)
        );

        renumberActiveEntries(appointmentId, principal.getStudioId());

        return new PromoteWaitlistEntryResponse(
                toAppointmentDetailsResponse(appointment),
                list(principal, appointmentId)
        );
    }

    private void validateCanModifyWaitlist(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_NOT_ALLOWED_STATUS",
                    "Nu poți modifica lista de așteptare a unei programări anulate."
            );
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_NOT_ALLOWED_STATUS",
                    "Nu poți modifica lista de așteptare a unei programări închise."
            );
        }

        if (!appointment.getStartAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_AFTER_START_FORBIDDEN",
                    "Nu poți modifica lista de așteptare după începerea programării."
            );
        }
    }

    private void validateAppointmentIsFull(Appointment appointment) {
        Integer capacity = appointment.getCapacity();

        if (capacity == null) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_CAPACITY_UNKNOWN",
                    "Programarea nu poate folosi lista de așteptare fără capacitate definită."
            );
        }

        int participantCount = appointment.getParticipants().size();

        if (participantCount < capacity) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_NOT_FULL",
                    "Programarea nu este plină. Adaugă clientul direct ca participant."
            );
        }
    }

    private void ensureCapacityHasFreeSlotForPromotion(Appointment appointment) {
        Integer capacity = appointment.getCapacity();

        if (capacity == null) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_CAPACITY_UNKNOWN",
                    "Programarea nu poate promova din lista de așteptare fără capacitate definită."
            );
        }

        if (appointment.getParticipants().size() >= capacity) {
            throw new ConflictException(
                    "APPOINTMENT_WAITLIST_PROMOTION_NO_FREE_SLOT",
                    "Nu există niciun loc liber pentru promovare din lista de așteptare."
            );
        }
    }

    private void renumberActiveEntries(UUID appointmentId, UUID studioId) {
        List<WaitlistEntry> activeEntries = waitlistEntryRepository
                .findByAppointmentIdAndStudioIdAndStatusOrderByPositionAsc(
                        appointmentId,
                        studioId,
                        WaitlistStatus.ACTIVE
                );

        for (int i = 0; i < activeEntries.size(); i++) {
            activeEntries.get(i).setPosition(i + 1);
        }
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry entry) {
        return new WaitlistEntryResponse(
                entry.getId(),
                entry.getPosition(),
                entry.getClient().getId(),
                entry.getClient().getFullName().trim(),
                entry.getStatus().name(),
                entry.getCreatedAt()
        );
    }

    private AppointmentDetailsResponse toAppointmentDetailsResponse(Appointment appointment) {
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

    private Map<String, Object> addAuditSummary(WaitlistEntry entry) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("waitlistEntryId", entry.getId().toString());
        summary.put("clientId", entry.getClient().getId().toString());
        summary.put("clientName", entry.getClient().getFullName().trim());
        summary.put("position", entry.getPosition());
        summary.put("status", entry.getStatus().name());
        return summary;
    }

    private Map<String, Object> removeAuditSummary(WaitlistEntry entry) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("waitlistEntryId", entry.getId().toString());
        summary.put("clientId", entry.getClient().getId().toString());
        summary.put("clientName", entry.getClient().getFullName().trim());
        summary.put("position", entry.getPosition());
        summary.put("status", entry.getStatus().name());
        return summary;
    }

    private Map<String, Object> promoteAuditSummary(WaitlistEntry entry, Appointment appointment) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("waitlistEntryId", entry.getId().toString());
        summary.put("clientId", entry.getClient().getId().toString());
        summary.put("clientName", entry.getClient().getFullName().trim());
        summary.put("promotedFromPosition", entry.getPosition());
        summary.put("waitlistStatus", entry.getStatus().name());
        summary.put("participantCountAfterPromotion", appointment.getParticipants().size());
        summary.put("capacity", appointment.getCapacity());
        return summary;
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
}