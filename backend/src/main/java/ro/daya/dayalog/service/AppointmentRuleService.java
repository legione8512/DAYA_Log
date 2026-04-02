package ro.daya.dayalog.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.Instructor;
import ro.daya.dayalog.entity.InstructorWorkingHours;
import ro.daya.dayalog.entity.ResourceEntity;
import ro.daya.dayalog.entity.ServiceEntity;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.repository.InstructorRepository;
import ro.daya.dayalog.repository.InstructorWorkingHoursRepository;
import ro.daya.dayalog.repository.ResourceEntityRepository;
import ro.daya.dayalog.repository.ServiceEntityRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.BadRequestException;

@Service
public class AppointmentRuleService {

    private final ServiceEntityRepository serviceEntityRepository;
    private final InstructorRepository instructorRepository;
    private final ResourceEntityRepository resourceEntityRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final InstructorWorkingHoursRepository instructorWorkingHoursRepository;

    public AppointmentRuleService(ServiceEntityRepository serviceEntityRepository,
                                  InstructorRepository instructorRepository,
                                  ResourceEntityRepository resourceEntityRepository,
                                  ClientRepository clientRepository,
                                  AppointmentRepository appointmentRepository,
                                  InstructorWorkingHoursRepository instructorWorkingHoursRepository) {
        this.serviceEntityRepository = serviceEntityRepository;
        this.instructorRepository = instructorRepository;
        this.resourceEntityRepository = resourceEntityRepository;
        this.clientRepository = clientRepository;
        this.appointmentRepository = appointmentRepository;
        this.instructorWorkingHoursRepository = instructorWorkingHoursRepository;
    }

    @Transactional(readOnly = true)
    public ResolvedAppointmentData validateAndResolve(CurrentUserPrincipal principal,
                                                      String appointmentTypeValue,
                                                      UUID serviceId,
                                                      UUID instructorId,
                                                      UUID resourceId,
                                                      OffsetDateTime startAt,
                                                      OffsetDateTime endAt,
                                                      String statusValue,
                                                      List<UUID> participantClientIds,
                                                      Integer capacity,
                                                      UUID ignoreAppointmentId) {

        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
        	throw new BadRequestException(
        	        "APPOINTMENT_TIME_RANGE_INVALID",
        	        "Ora de sfârșit trebuie să fie după ora de început."
        	);        }

        if (participantClientIds == null || participantClientIds.isEmpty()) {
        	throw new BadRequestException(
        	        "APPOINTMENT_PARTICIPANTS_REQUIRED",
        	        "Selectează cel puțin un client."
        	);        }

        AppointmentType appointmentType = parseAppointmentType(appointmentTypeValue);
        AppointmentStatus status = parseAppointmentStatus(statusValue);

        if (status == AppointmentStatus.CANCELLED) {
        	throw new BadRequestException(
        	        "APPOINTMENT_CANCEL_ENDPOINT_REQUIRED",
        	        "Folosește endpointul de anulare pentru programările anulate."
        	);        }

        ServiceEntity service = serviceEntityRepository.findByIdAndStudioId(serviceId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("SERVICE_NOT_FOUND", "Serviciul nu a fost găsit."));

        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new ConflictException("SERVICE_INACTIVE", "Serviciul selectat este inactiv.");
        }

        Instructor instructor = instructorRepository.findByIdAndStudioId(instructorId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR_NOT_FOUND", "Instructorul nu a fost găsit."));

        if (!Boolean.TRUE.equals(instructor.getActive())) {
            throw new ConflictException("INSTRUCTOR_INACTIVE", "Instructorul selectat este inactiv.");
        }

        validateInstructorWorkingHours(principal.getStudioId(), instructorId, startAt, endAt);

        ResourceEntity resource = null;
        if (resourceId != null) {
            resource = resourceEntityRepository.findByIdAndStudioId(resourceId, principal.getStudioId())
                    .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Resursa nu a fost găsită."));

            if (!Boolean.TRUE.equals(resource.getActive())) {
                throw new ConflictException("RESOURCE_INACTIVE", "Resursa selectată este inactivă.");
            }
        }

        List<Client> participants = resolveParticipants(principal.getStudioId(), participantClientIds);

        if (appointmentType == AppointmentType.INDIVIDUAL) {
            if (participants.size() != 1) {
            	throw new BadRequestException(
            	        "APPOINTMENT_INDIVIDUAL_PARTICIPANT_COUNT_INVALID",
            	        "Programarea individuală poate avea un singur client."
            	);            }
            if (capacity == null || capacity != 1) {
            	throw new BadRequestException(
            	        "APPOINTMENT_INDIVIDUAL_CAPACITY_INVALID",
            	        "Programarea individuală trebuie să aibă capacitate 1."
            	);            }
        }

        if (appointmentType == AppointmentType.GROUP) {
            if (capacity == null || capacity < 2) {
            	throw new BadRequestException(
            	        "APPOINTMENT_GROUP_CAPACITY_MIN_INVALID",
            	        "Programarea de grup trebuie să aibă o capacitate de cel puțin 2."
            	);            }
            if (participants.size() > capacity) {
            	throw new BadRequestException(
            	        "APPOINTMENT_GROUP_CAPACITY_TOO_SMALL",
            	        "Capacitatea este prea mică pentru numărul de participanți."
            	);            }
        }

        if (appointmentType == AppointmentType.GROUP) {
            List<Appointment> compatibleGroupSessions = appointmentRepository.findCompatibleGroupSessions(
                    principal.getStudioId(),
                    serviceId,
                    instructorId,
                    startAt,
                    endAt,
                    ignoreAppointmentId,
                    AppointmentType.GROUP,
                    AppointmentStatus.CANCELLED
            );

            if (!compatibleGroupSessions.isEmpty()) {
                Appointment existingGroupSession = compatibleGroupSessions.get(0);

                throw new ConflictException(
                        "APPOINTMENT_COMPATIBLE_GROUP_EXISTS",
                        "Există deja o sesiune de grup compatibilă. Adaugă clientul la sesiunea existentă.",
                        buildCompatibleGroupDetails(existingGroupSession)
                );
            }
        }

        if (!appointmentRepository.findInstructorOverlaps(
                principal.getStudioId(),
                instructorId,
                startAt,
                endAt,
                ignoreAppointmentId,
                AppointmentStatus.CANCELLED
        ).isEmpty()) {
            throw new ConflictException(
                    "APPOINTMENT_INSTRUCTOR_CONFLICT",
                    "Instructorul are deja o programare în acest interval."
            );
        }

        if (resource != null && !appointmentRepository.findResourceOverlaps(
                principal.getStudioId(),
                resourceId,
                startAt,
                endAt,
                ignoreAppointmentId,
                AppointmentStatus.CANCELLED
        ).isEmpty()) {
            throw new ConflictException(
                    "APPOINTMENT_RESOURCE_CONFLICT",
                    "Resursa este deja ocupată în acest interval."
            );
        }

        for (Client client : participants) {
            if (!appointmentRepository.findClientOverlaps(
                    principal.getStudioId(),
                    client.getId(),
                    startAt,
                    endAt,
                    ignoreAppointmentId,
                    AppointmentStatus.CANCELLED
            ).isEmpty()) {
                throw new ConflictException(
                        "APPOINTMENT_CLIENT_CONFLICT",
                        "Clientul " + client.getFullName().trim() + " are deja o programare în acest interval."
                );
            }
        }

        return new ResolvedAppointmentData(
                appointmentType,
                status,
                service,
                instructor,
                resource,
                participants,
                capacity,
                startAt,
                endAt
        );
    }

    @Transactional(readOnly = true)
    public void validateCancellation(Appointment appointment) {
        if (appointment.isCancelled()) {
            throw new ConflictException("APPOINTMENT_ALREADY_CANCELLED", "Programarea este deja anulată.");
        }

        OffsetDateTime deadline = appointment.getStartAt().minusHours(3);
        if (OffsetDateTime.now().isAfter(deadline)) {
            throw new ConflictException(
                    "APPOINTMENT_CANCELLATION_WINDOW_PASSED",
                    "Programarea poate fi anulată doar cu cel puțin 3 ore înainte."
            );
        }
    }

    private List<Client> resolveParticipants(UUID studioId, List<UUID> participantClientIds) {
        Set<UUID> uniqueIds = new HashSet<>(participantClientIds);
        if (uniqueIds.size() != participantClientIds.size()) {
        	throw new BadRequestException(
        	        "APPOINTMENT_PARTICIPANT_DUPLICATES",
        	        "Lista participanților conține duplicate."
        	);        }

        List<Client> result = new ArrayList<>();

        for (UUID clientId : participantClientIds) {
            Client client = clientRepository.findByIdAndStudioId(clientId, studioId)
                    .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Clientul nu a fost găsit."));

            if (!Boolean.TRUE.equals(client.getActive())) {
                throw new ConflictException(
                        "CLIENT_INACTIVE",
                        "Clientul " + client.getFullName().trim() + " este inactiv."
                );
            }

            result.add(client);
        }

        return result;
    }

    private AppointmentType parseAppointmentType(String value) {
        if (value == null || value.isBlank()) {
        	throw new BadRequestException(
        	        "APPOINTMENT_TYPE_REQUIRED",
        	        "Tipul programării este obligatoriu."
        	);        }

        try {
            return AppointmentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_TYPE_INVALID",
        	        "Tip invalid pentru programare."
        	);        }
    }

    private AppointmentStatus parseAppointmentStatus(String value) {
        if (value == null || value.isBlank()) {
        	throw new BadRequestException(
        	        "APPOINTMENT_STATUS_REQUIRED",
        	        "Statusul programării este obligatoriu."
        	);        }

        try {
            return AppointmentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "APPOINTMENT_STATUS_INVALID",
        	        "Status invalid pentru programare."
        	);        }
    }

    private void validateInstructorWorkingHours(UUID studioId,
                                                UUID instructorId,
                                                OffsetDateTime startAt,
                                                OffsetDateTime endAt) {
        if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
            throw new ConflictException(
                    "APPOINTMENT_SPANS_MULTIPLE_DAYS",
                    "Programarea trebuie să fie în aceeași zi pentru validarea programului instructorului."
            );
        }

        DayOfWeek dayOfWeek = startAt.getDayOfWeek();
        LocalTime requestedStart = startAt.toLocalTime();
        LocalTime requestedEnd = endAt.toLocalTime();

        List<InstructorWorkingHours> workingHours = instructorWorkingHoursRepository.findActiveForDay(
                studioId,
                instructorId,
                dayOfWeek
        );

        if (workingHours.isEmpty()) {
            return;
        }

        boolean fitsAnyInterval = workingHours.stream().anyMatch(interval ->
                !requestedStart.isBefore(interval.getStartTime())
                        && !requestedEnd.isAfter(interval.getEndTime())
        );

        if (!fitsAnyInterval) {
            throw new ConflictException(
                    "INSTRUCTOR_OUTSIDE_WORKING_HOURS",
                    "Programarea este în afara intervalului de lucru al instructorului."
            );
        }
    }

    private Map<String, Object> buildCompatibleGroupDetails(Appointment appointment) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("existingAppointmentId", appointment.getId().toString());
        details.put("appointmentType", appointment.getAppointmentType().name());
        details.put("status", appointment.getStatus().name());
        details.put("serviceId", appointment.getService().getId().toString());
        details.put("serviceName", appointment.getService().getName());
        details.put("instructorId", appointment.getInstructor().getId().toString());
        details.put("instructorName", appointment.getInstructor().getFullName());
        details.put("resourceId", appointment.getResource() == null ? null : appointment.getResource().getId().toString());
        details.put("resourceName", appointment.getResource() == null ? null : appointment.getResource().getName());
        details.put("startAt", appointment.getStartAt().toString());
        details.put("endAt", appointment.getEndAt().toString());
        details.put("capacity", appointment.getCapacity());
        details.put("participantCount", appointment.getParticipants() == null ? 0 : appointment.getParticipants().size());
        return details;
    }

    public record ResolvedAppointmentData(
            AppointmentType appointmentType,
            AppointmentStatus status,
            ServiceEntity service,
            Instructor instructor,
            ResourceEntity resource,
            List<Client> participants,
            Integer capacity,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
    }
}