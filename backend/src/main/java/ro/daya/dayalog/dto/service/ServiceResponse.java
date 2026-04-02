package ro.daya.dayalog.dto.service;

import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String name,
        String description,
        Integer defaultDurationMinutes,
        Boolean active
) {
}