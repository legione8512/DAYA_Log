package ro.daya.dayalog.dto.client;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientAppointmentTimelineItemResponse(
        UUID id,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String appointmentType,
        String status,
        String serviceName,
        String instructorName,
        String resourceName
) {
}