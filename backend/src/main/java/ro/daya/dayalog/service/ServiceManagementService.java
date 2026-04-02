package ro.daya.dayalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.common.StatusToggleRequest;
import ro.daya.dayalog.dto.service.ServiceRequest;
import ro.daya.dayalog.dto.service.ServiceResponse;
import ro.daya.dayalog.entity.ServiceEntity;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.repository.ServiceEntityRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;

@Service
public class ServiceManagementService {

    private final ServiceEntityRepository serviceEntityRepository;
    private final AuditLogService auditLogService;

    public ServiceManagementService(ServiceEntityRepository serviceEntityRepository,
                                    AuditLogService auditLogService) {
        this.serviceEntityRepository = serviceEntityRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> list(CurrentUserPrincipal principal, String query, Boolean active) {
        String normalizedQuery = normalizeQuery(query);

        List<ServiceEntity> entities;

        if (normalizedQuery == null) {
            entities = (active == null)
                    ? serviceEntityRepository.findByStudioIdOrderByNameAsc(principal.getStudioId())
                    : serviceEntityRepository.findByStudioIdAndActiveOrderByNameAsc(principal.getStudioId(), active);
        } else {
            entities = serviceEntityRepository.searchByStudioIdAndFilters(
                    principal.getStudioId(),
                    normalizedQuery,
                    active
            );
        }

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ServiceResponse create(CurrentUserPrincipal principal, ServiceRequest request) {
        String normalizedName = request.name().trim();

        if (serviceEntityRepository.existsByStudioIdAndNameIgnoreCase(principal.getStudioId(), normalizedName)) {
            throw new ConflictException("SERVICE_NAME_CONFLICT", "Există deja un serviciu cu acest nume.");
        }

        ServiceEntity entity = new ServiceEntity();
        entity.setStudio(studioRef(principal.getStudioId()));
        entity.setName(normalizedName);
        entity.setDescription(blankToNull(request.description()));
        entity.setDefaultDurationMinutes(request.defaultDurationMinutes() == null ? 60 : request.defaultDurationMinutes());
        entity.setActive(true);

        ServiceEntity saved = serviceEntityRepository.save(entity);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "service",
                saved.getId(),
                "CREATE_SERVICE",
                serviceAuditSummary(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public ServiceResponse update(CurrentUserPrincipal principal, UUID id, ServiceRequest request) {
        ServiceEntity entity = serviceEntityRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("SERVICE_NOT_FOUND", "Serviciul nu a fost găsit."));

        String normalizedName = request.name().trim();

        if (serviceEntityRepository.existsByStudioIdAndNameIgnoreCaseAndIdNot(principal.getStudioId(), normalizedName, id)) {
            throw new ConflictException("SERVICE_NAME_CONFLICT", "Există deja un serviciu cu acest nume.");
        }

        Map<String, Object> before = serviceAuditSummary(entity);

        entity.setName(normalizedName);
        entity.setDescription(blankToNull(request.description()));
        entity.setDefaultDurationMinutes(request.defaultDurationMinutes() == null ? 60 : request.defaultDurationMinutes());

        Map<String, Object> after = serviceAuditSummary(entity);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "service",
                entity.getId(),
                "UPDATE_SERVICE",
                beforeAfterSummary(before, after)
        );

        return toResponse(entity);
    }

    @Transactional
    public void updateStatus(CurrentUserPrincipal principal, UUID id, StatusToggleRequest request) {
        ServiceEntity entity = serviceEntityRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("SERVICE_NOT_FOUND", "Serviciul nu a fost găsit."));

        Boolean previousActive = entity.getActive();
        entity.setActive(request.active());

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", entity.getName());
        summary.put("beforeActive", previousActive);
        summary.put("afterActive", entity.getActive());

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "service",
                entity.getId(),
                "UPDATE_SERVICE_STATUS",
                summary
        );
    }

    private ServiceResponse toResponse(ServiceEntity entity) {
        return new ServiceResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDefaultDurationMinutes(),
                entity.getActive()
        );
    }

    private Map<String, Object> serviceAuditSummary(ServiceEntity entity) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", entity.getName());
        summary.put("description", entity.getDescription());
        summary.put("defaultDurationMinutes", entity.getDefaultDurationMinutes());
        summary.put("active", entity.getActive());
        return summary;
    }

    private Map<String, Object> beforeAfterSummary(Map<String, Object> before,
                                                   Map<String, Object> after) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("before", before);
        summary.put("after", after);
        return summary;
    }

    private Studio studioRef(UUID studioId) {
        Studio studio = new Studio();
        studio.setId(studioId);
        return studio;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}