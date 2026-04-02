package ro.daya.dayalog.dto.appointment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WaitlistEntryResponse(
        UUID id,
        Integer position,
        UUID clientId,
        String clientFullName,
        String status,
        OffsetDateTime createdAt
) {
}