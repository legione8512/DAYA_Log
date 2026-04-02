package ro.daya.dayalog.dto.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentRequest(
        @NotNull String appointmentType,
        @NotNull UUID serviceId,
        @NotNull UUID instructorId,
        UUID resourceId,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        @NotNull String status,
        String notes,
        @NotEmpty List<UUID> participantClientIds,
        @NotNull Integer capacity
) {
}