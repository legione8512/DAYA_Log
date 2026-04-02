package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PromoteWaitlistEntryRequest(
        @NotNull UUID waitlistEntryId
) {
}