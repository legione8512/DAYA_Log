package ro.daya.dayalog.dto.common;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogDetailsResponse(
        UUID id,
        UUID studioId,
        String entityName,
        UUID entityId,
        String action,
        UUID actorUserId,
        String actorEmail,
        String actorRole,
        Map<String, Object> changeSummary,
        OffsetDateTime createdAt
) {
}