package ro.daya.dayalog.dto.appointment;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentListResponseItem(
        UUID id,
        LocalDate date,
        String timeRange,
        String appointmentType,
        String serviceName,
        String instructorName,
        int participantCount,
        String status
) {
}