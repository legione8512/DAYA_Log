package ro.daya.dayalog.dto.appointment;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AddAppointmentParticipantsRequest(
        @NotEmpty List<@NotNull UUID> participantClientIds
) {
}