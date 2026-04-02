package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

public record ResourceOptionResponse(
        UUID id,
        String name,
        String type
) {
}