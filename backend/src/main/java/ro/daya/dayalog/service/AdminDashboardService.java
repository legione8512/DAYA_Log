package ro.daya.dayalog.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.common.AdminDashboardResponse;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.repository.AppointmentRepository;
import ro.daya.dayalog.repository.ClientRepository;
import ro.daya.dayalog.repository.InstructorRepository;
import ro.daya.dayalog.repository.ResourceEntityRepository;
import ro.daya.dayalog.repository.ServiceEntityRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class AdminDashboardService {

    private static final ZoneId DEFAULT_STUDIO_ZONE = ZoneId.of("Europe/Bucharest");

    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final InstructorRepository instructorRepository;
    private final ResourceEntityRepository resourceEntityRepository;

    public AdminDashboardService(ClientRepository clientRepository,
                                 AppointmentRepository appointmentRepository,
                                 ServiceEntityRepository serviceEntityRepository,
                                 InstructorRepository instructorRepository,
                                 ResourceEntityRepository resourceEntityRepository) {
        this.clientRepository = clientRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceEntityRepository = serviceEntityRepository;
        this.instructorRepository = instructorRepository;
        this.resourceEntityRepository = resourceEntityRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(CurrentUserPrincipal principal) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = LocalDate.now(DEFAULT_STUDIO_ZONE)
                .atStartOfDay(DEFAULT_STUDIO_ZONE)
                .toOffsetDateTime();
        OffsetDateTime tomorrowStart = LocalDate.now(DEFAULT_STUDIO_ZONE)
                .plusDays(1)
                .atStartOfDay(DEFAULT_STUDIO_ZONE)
                .toOffsetDateTime();

        long activeClientsCount = clientRepository.countByStudioIdAndActive(principal.getStudioId(), true);

        long todayAppointmentsCount = appointmentRepository.countDashboardAppointmentsBetween(
                principal.getStudioId(),
                todayStart,
                tomorrowStart,
                AppointmentStatus.CANCELLED
        );

        long upcomingAppointmentsCount = appointmentRepository.countDashboardUpcomingAppointments(
                principal.getStudioId(),
                now,
                AppointmentStatus.CANCELLED
        );

        long activeServicesCount = serviceEntityRepository.countByStudioIdAndActive(principal.getStudioId(), true);
        long activeInstructorsCount = instructorRepository.countByStudioIdAndActive(principal.getStudioId(), true);
        long activeResourcesCount = resourceEntityRepository.countByStudioIdAndActive(principal.getStudioId(), true);

        return new AdminDashboardResponse(
                activeClientsCount,
                todayAppointmentsCount,
                upcomingAppointmentsCount,
                activeServicesCount,
                activeInstructorsCount,
                activeResourcesCount
        );
    }
}