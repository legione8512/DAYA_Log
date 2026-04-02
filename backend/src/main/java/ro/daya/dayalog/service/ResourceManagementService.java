package ro.daya.dayalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.common.StatusToggleRequest;
import ro.daya.dayalog.dto.resource.ResourceRequest;
import ro.daya.dayalog.dto.resource.ResourceResponse;
import ro.daya.dayalog.entity.ResourceEntity;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.enums.ResourceType;
import ro.daya.dayalog.repository.ResourceEntityRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;

@Service
public class ResourceManagementService {

    private final ResourceEntityRepository resourceEntityRepository;
    private final AuditLogService auditLogService;

    public ResourceManagementService(ResourceEntityRepository resourceEntityRepository,
                                     AuditLogService auditLogService) {
        this.resourceEntityRepository = resourceEntityRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> list(CurrentUserPrincipal principal, String query, Boolean active) {
        String normalizedQuery = normalizeQuery(query);

        List<ResourceEntity> entities;

        if (normalizedQuery == null) {
            entities = (active == null)
                    ? resourceEntityRepository.findByStudioIdOrderByNameAsc(principal.getStudioId())
                    : resourceEntityRepository.findByStudioIdAndActiveOrderByNameAsc(principal.getStudioId(), active);
        } else {
            entities = resourceEntityRepository.searchByStudioIdAndFilters(
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
    public ResourceResponse create(CurrentUserPrincipal principal, ResourceRequest request) {
        String normalizedName = request.name().trim();

        if (resourceEntityRepository.existsByStudioIdAndNameIgnoreCase(principal.getStudioId(), normalizedName)) {
            throw new ConflictException("RESOURCE_NAME_CONFLICT", "Există deja o resursă cu acest nume.");
        }

        ResourceEntity entity = new ResourceEntity();
        entity.setStudio(studioRef(principal.getStudioId()));
        entity.setName(normalizedName);
        entity.setType(parseType(request.type()));
        entity.setNotes(blankToNull(request.notes()));
        entity.setActive(true);

        ResourceEntity saved = resourceEntityRepository.save(entity);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "resource",
                saved.getId(),
                "CREATE_RESOURCE",
                resourceAuditSummary(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public ResourceResponse update(CurrentUserPrincipal principal, UUID id, ResourceRequest request) {
        ResourceEntity entity = resourceEntityRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Resursa nu a fost găsită."));

        String normalizedName = request.name().trim();

        if (resourceEntityRepository.existsByStudioIdAndNameIgnoreCaseAndIdNot(principal.getStudioId(), normalizedName, id)) {
            throw new ConflictException("RESOURCE_NAME_CONFLICT", "Există deja o resursă cu acest nume.");
        }

        Map<String, Object> before = resourceAuditSummary(entity);

        entity.setName(normalizedName);
        entity.setType(parseType(request.type()));
        entity.setNotes(blankToNull(request.notes()));

        Map<String, Object> after = resourceAuditSummary(entity);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "resource",
                entity.getId(),
                "UPDATE_RESOURCE",
                beforeAfterSummary(before, after)
        );

        return toResponse(entity);
    }

    @Transactional
    public void updateStatus(CurrentUserPrincipal principal, UUID id, StatusToggleRequest request) {
        ResourceEntity entity = resourceEntityRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Resursa nu a fost găsită."));

        Boolean previousActive = entity.getActive();
        entity.setActive(request.active());

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", entity.getName());
        summary.put("type", entity.getType().name());
        summary.put("beforeActive", previousActive);
        summary.put("afterActive", entity.getActive());

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "resource",
                entity.getId(),
                "UPDATE_RESOURCE_STATUS",
                summary
        );
    }

    private ResourceResponse toResponse(ResourceEntity entity) {
        return new ResourceResponse(
                entity.getId(),
                entity.getName(),
                entity.getType().name(),
                entity.getNotes(),
                entity.getActive()
        );
    }

    private Map<String, Object> resourceAuditSummary(ResourceEntity entity) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", entity.getName());
        summary.put("type", entity.getType().name());
        summary.put("notes", entity.getNotes());
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

    private ResourceType parseType(String value) {
        return ResourceType.valueOf(value.trim().toUpperCase());
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