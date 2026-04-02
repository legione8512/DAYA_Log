package ro.daya.dayalog.service;

import java.time.OffsetDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.client.ClientDashboardNextAppointmentResponse;
import ro.daya.dayalog.dto.client.ClientDashboardResponse;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class ClientDashboardService {

    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;

    public ClientDashboardService(ClientRepository clientRepository,
                                  AppointmentRepository appointmentRepository) {
        this.clientRepository = clientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public ClientDashboardResponse getDashboard(CurrentUserPrincipal principal) {
        Client client = clientRepository.findByUserIdAndStudioId(principal.getId(), principal.getStudioId())
                .orElseThrow(() -> new NotFoundException(
                        "CLIENT_PROFILE_NOT_FOUND",
                        "Profilul clientului nu a fost găsit."
                ));

        OffsetDateTime now = OffsetDateTime.now();

        long futureAppointmentsCount = appointmentRepository.countFutureForClientUser(
                principal.getStudioId(),
                principal.getId(),
                now,
                AppointmentStatus.CANCELLED
        );

        long historyAppointmentsCount = appointmentRepository.countHistoryForClientUser(
                principal.getStudioId(),
                principal.getId(),
                now,
                AppointmentStatus.CANCELLED
        );

        Appointment nextAppointment = appointmentRepository.findNextForClientUser(
                principal.getStudioId(),
                principal.getId(),
                now,
                AppointmentStatus.CANCELLED,
                PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);

        return new ClientDashboardResponse(
                client.getId(),
                client.getFirstName(),
                client.getFullName().trim(),
                futureAppointmentsCount,
                historyAppointmentsCount,
                toNextAppointmentResponse(nextAppointment)
        );
    }

    private ClientDashboardNextAppointmentResponse toNextAppointmentResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return new ClientDashboardNextAppointmentResponse(
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
}