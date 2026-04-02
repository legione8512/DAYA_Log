package ro.daya.dayalog.dto.common;

import jakarta.validation.constraints.NotNull;

public record StatusToggleRequest(
        @NotNull Boolean active
) {
}