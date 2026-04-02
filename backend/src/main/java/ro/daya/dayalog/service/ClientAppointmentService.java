package ro.daya.dayalog.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.appointment.ClientAppointmentResponse;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.BadRequestException;

@Service
public class ClientAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;

    public ClientAppointmentService(AppointmentRepository appointmentRepository,
                                    ClientRepository clientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientAppointmentResponse> future(CurrentUserPrincipal principal) {
        ensureLinkedClientExists(principal);

        return appointmentRepository.findFutureForClientUser(
                        principal.getStudioId(),
                        principal.getId(),
                        OffsetDateTime.now(),
                        AppointmentStatus.CANCELLED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientAppointmentResponse> history(CurrentUserPrincipal principal) {
        ensureLinkedClientExists(principal);

        return appointmentRepository.findHistoryForClientUser(
                        principal.getStudioId(),
                        principal.getId(),
                        OffsetDateTime.now(),
                        AppointmentStatus.CANCELLED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureLinkedClientExists(CurrentUserPrincipal principal) {
        clientRepository.findByUserIdAndStudioId(principal.getId(), principal.getStudioId())
                .orElseThrow(() -> new BadRequestException(
                        "CLIENT_PROFILE_NOT_LINKED",
                        "Nu există un profil de client legat de acest cont."
                ));
    }

    private ClientAppointmentResponse toResponse(Appointment appointment) {
        return new ClientAppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentType().name(),
                appointment.getService().getName(),
                appointment.getInstructor().getFullName(),
                appointment.getResource() == null ? null : appointment.getResource().getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus().name(),
                appointment.getNotes()
        );
    }
}