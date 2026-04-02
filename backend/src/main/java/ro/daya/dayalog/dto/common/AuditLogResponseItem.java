package ro.daya.dayalog.dto.common;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponseItem(
        UUID id,
        String entityName,
        UUID entityId,
        String action,
        UUID actorUserId,
        String actorEmail,
        Map<String, Object> changeSummary,
        OffsetDateTime createdAt
) {
}