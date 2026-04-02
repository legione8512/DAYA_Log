package ro.daya.dayalog.dto.appointment;

import java.util.List;

public record PromoteWaitlistEntryResponse(
        AppointmentDetailsResponse appointment,
        List<WaitlistEntryResponse> waitlist
) {
}