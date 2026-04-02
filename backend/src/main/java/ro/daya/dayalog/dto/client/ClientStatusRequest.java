package ro.daya.dayalog.dto.client;

import jakarta.validation.constraints.NotNull;

public record ClientStatusRequest(
        @NotNull Boolean active
) {
}