package ro.daya.dayalog.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull String type,
        String notes
) {
}