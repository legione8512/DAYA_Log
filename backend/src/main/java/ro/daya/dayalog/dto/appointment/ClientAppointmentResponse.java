package ro.daya.dayalog.dto.appointment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientAppointmentResponse(
        UUID id,
        String appointmentType,
        String serviceName,
        String instructorName,
        String resourceName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        String notes
) {
}