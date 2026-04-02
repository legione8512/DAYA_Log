package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

public record ServiceOptionResponse(
        UUID id,
        String name,
        Integer defaultDurationMinutes
) {
}