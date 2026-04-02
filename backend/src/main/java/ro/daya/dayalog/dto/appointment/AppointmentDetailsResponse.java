package ro.daya.dayalog.dto.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentDetailsResponse(
        UUID id,
        String appointmentType,
        UUID serviceId,
        String serviceName,
        UUID instructorId,
        String instructorName,
        UUID resourceId,
        String resourceName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        Integer capacity,
        String notes,
        List<AppointmentParticipantResponse> participants
) {
}