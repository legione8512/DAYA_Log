package ro.daya.dayalog.dto.resource;

import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String name,
        String type,
        String notes,
        Boolean active
) {
}