package ro.daya.dayalog.dto.client;

import java.util.UUID;

public record ClientDashboardResponse(
        UUID clientId,
        String firstName,
        String fullName,
        long futureAppointmentsCount,
        long historyAppointmentsCount,
        ClientDashboardNextAppointmentResponse nextAppointment
) {
}