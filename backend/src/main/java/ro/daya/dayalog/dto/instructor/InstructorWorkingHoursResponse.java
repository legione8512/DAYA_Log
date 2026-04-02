package ro.daya.dayalog.dto.instructor;

import java.util.List;
import java.util.UUID;

public record InstructorWorkingHoursResponse(
        UUID instructorId,
        String instructorFullName,
        List<InstructorWorkingHoursEntryResponse> workingHours
) {
}