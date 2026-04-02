package ro.daya.dayalog.dto.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Min(15) @Max(240) Integer defaultDurationMinutes
) {
}