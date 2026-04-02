package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RemoveWaitlistEntryRequest(
        @NotNull UUID waitlistEntryId
) {
}