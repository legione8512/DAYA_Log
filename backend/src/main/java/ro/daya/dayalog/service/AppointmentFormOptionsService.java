package ro.daya.dayalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.appointment.AppointmentFormOptionsResponse;
import ro.daya.dayalog.dto.appointment.InstructorOptionResponse;
import ro.daya.dayalog.dto.appointment.ResourceOptionResponse;
import ro.daya.dayalog.dto.appointment.ServiceOptionResponse;
import ro.daya.dayalog.repository.InstructorRepository;
import ro.daya.dayalog.repository.ResourceEntityRepository;
import ro.daya.dayalog.repository.ServiceEntityRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class AppointmentFormOptionsService {

    private final ServiceEntityRepository serviceEntityRepository;
    private final InstructorRepository instructorRepository;
    private final ResourceEntityRepository resourceEntityRepository;

    public AppointmentFormOptionsService(ServiceEntityRepository serviceEntityRepository,
                                         InstructorRepository instructorRepository,
                                         ResourceEntityRepository resourceEntityRepository) {
        this.serviceEntityRepository = serviceEntityRepository;
        this.instructorRepository = instructorRepository;
        this.resourceEntityRepository = resourceEntityRepository;
    }

    @Transactional(readOnly = true)
    public AppointmentFormOptionsResponse getFormOptions(CurrentUserPrincipal principal) {
        List<ServiceOptionResponse> services = serviceEntityRepository
                .findByStudioIdAndActiveOrderByNameAsc(principal.getStudioId(), true)
                .stream()
                .map(service -> new ServiceOptionResponse(
                        service.getId(),
                        service.getName(),
                        service.getDefaultDurationMinutes()
                ))
                .toList();

        List<InstructorOptionResponse> instructors = instructorRepository
                .findByStudioIdAndActiveOrderByLastNameAscFirstNameAsc(principal.getStudioId(), true)
                .stream()
                .map(instructor -> new InstructorOptionResponse(
                        instructor.getId(),
                        instructor.getFullName()
                ))
                .toList();

        List<ResourceOptionResponse> resources = resourceEntityRepository
                .findByStudioIdAndActiveOrderByNameAsc(principal.getStudioId(), true)
                .stream()
                .map(resource -> new ResourceOptionResponse(
                        resource.getId(),
                        resource.getName(),
                        resource.getType().name()
                ))
                .toList();

        return new AppointmentFormOptionsResponse(services, instructors, resources);
    }
}