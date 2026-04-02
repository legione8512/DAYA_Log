package ro.daya.dayalog.dto.instructor;

import java.util.UUID;

public record InstructorResponse(
        UUID id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        Boolean active
) {
}