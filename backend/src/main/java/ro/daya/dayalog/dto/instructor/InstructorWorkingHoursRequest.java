package ro.daya.dayalog.dto.instructor;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InstructorWorkingHoursRequest(
        @NotNull List<@Valid InstructorWorkingHoursEntryRequest> entries
) {
}