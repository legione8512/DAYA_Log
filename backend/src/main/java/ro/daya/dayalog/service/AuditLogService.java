package ro.daya.dayalog.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.common.AuditLogDetailsResponse;
import ro.daya.dayalog.dto.common.AuditLogFilterRequest;
import ro.daya.dayalog.dto.common.AuditLogResponseItem;
import ro.daya.dayalog.dto.common.PagedResponse;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.AuditLog;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.exception.BadRequestException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.AuditLogRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;

@Service
public class AuditLogService {

	private static final ZoneId DEFAULT_STUDIO_ZONE = ZoneId.of("Europe/Bucharest");

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void log(UUID studioId, UUID actorUserId, String entityName, UUID entityId, String action,
			Map<String, Object> changeSummary) {

		AuditLog auditLog = new AuditLog();
		auditLog.setStudio(studioRef(studioId));
		auditLog.setActorUser(userRef(actorUserId));
		auditLog.setEntityName(entityName);
		auditLog.setEntityId(entityId);
		auditLog.setAction(action);
		auditLog.setChangeSummary(changeSummary == null ? null : new LinkedHashMap<>(changeSummary));

		auditLogRepository.save(auditLog);
	}

	@Transactional(readOnly = true)
	public PagedResponse<AuditLogResponseItem> list(CurrentUserPrincipal principal, AuditLogFilterRequest request) {
		validateRequest(request);

		String entityName = normalizeEntityName(request.getEntityName());
		String action = normalizeAction(request.getAction());

		OffsetDateTime fromAt = request.getDateFrom() == null ? null : startOfDay(request.getDateFrom());
		OffsetDateTime toAt = request.getDateTo() == null ? null : startOfNextDay(request.getDateTo());

		List<AuditLog> filteredLogs = auditLogRepository.findForList(principal.getStudioId()).stream()
				.filter(auditLog -> matchesFilters(auditLog, entityName, action, fromAt, toAt)).toList();

		int page = request.getPage();
		int size = request.getSize();

		int fromIndex = Math.min(page * size, filteredLogs.size());
		int toIndex = Math.min(fromIndex + size, filteredLogs.size());

		int totalPages = filteredLogs.isEmpty() ? 0 : (int) Math.ceil((double) filteredLogs.size() / size);

		List<AuditLogResponseItem> content = filteredLogs.subList(fromIndex, toIndex).stream().map(this::toResponseItem)
				.toList();

		return new PagedResponse<>(content, page, size, filteredLogs.size(), totalPages);
	}

	private boolean matchesFilters(AuditLog auditLog, String entityName, String action, OffsetDateTime fromAt,
			OffsetDateTime toAt) {
		if (entityName != null && !auditLog.getEntityName().equalsIgnoreCase(entityName)) {
			return false;
		}

		if (action != null && !auditLog.getAction().equalsIgnoreCase(action)) {
			return false;
		}

		if (fromAt != null && auditLog.getCreatedAt().isBefore(fromAt)) {
			return false;
		}

		if (toAt != null && !auditLog.getCreatedAt().isBefore(toAt)) {
			return false;
		}

		return true;
	}

	@Transactional(readOnly = true)
	public AuditLogDetailsResponse getById(CurrentUserPrincipal principal, UUID id) {
		AuditLog auditLog = auditLogRepository.findByIdAndStudioId(id, principal.getStudioId()).orElseThrow(
				() -> new NotFoundException("AUDIT_LOG_NOT_FOUND", "Înregistrarea de audit nu a fost găsită."));

		return toDetailsResponse(auditLog);
	}

	private void validateRequest(AuditLogFilterRequest request) {
		if (request.getDateFrom() != null && request.getDateTo() != null
				&& request.getDateFrom().isAfter(request.getDateTo())) {
			throw new BadRequestException("AUDIT_LOG_DATE_FILTER_INVALID",
					"Filtrul de dată este invalid. dateFrom nu poate fi după dateTo.");
		}
	}

	private String normalizeEntityName(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toLowerCase();
	}

	private String normalizeAction(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toUpperCase();
	}

	private OffsetDateTime startOfDay(LocalDate date) {
		return date.atStartOfDay(DEFAULT_STUDIO_ZONE).toOffsetDateTime();
	}

	private OffsetDateTime startOfNextDay(LocalDate date) {
		return date.plusDays(1).atStartOfDay(DEFAULT_STUDIO_ZONE).toOffsetDateTime();
	}

	private AuditLogResponseItem toResponseItem(AuditLog auditLog) {
		return new AuditLogResponseItem(auditLog.getId(), auditLog.getEntityName(), auditLog.getEntityId(),
				auditLog.getAction(), auditLog.getActorUser().getId(), auditLog.getActorUser().getEmail(),
				auditLog.getChangeSummary(), auditLog.getCreatedAt());
	}

	private AuditLogDetailsResponse toDetailsResponse(AuditLog auditLog) {
		return new AuditLogDetailsResponse(auditLog.getId(), auditLog.getStudio().getId(), auditLog.getEntityName(),
				auditLog.getEntityId(), auditLog.getAction(), auditLog.getActorUser().getId(),
				auditLog.getActorUser().getEmail(), auditLog.getActorUser().getRole().name(),
				auditLog.getChangeSummary(), auditLog.getCreatedAt());
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