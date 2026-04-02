package ro.daya.dayalog.dto.instructor;

import java.time.LocalTime;

public record InstructorWorkingHoursEntryResponse(
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}