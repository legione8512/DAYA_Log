package ro.daya.dayalog.dto.appointment;

import java.util.UUID;

public record InstructorOptionResponse(
        UUID id,
        String fullName
) {
}