package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

public record AppointmentParticipantResponse(
        UUID clientId,
        String fullName,
        String participationStatus
) {
}