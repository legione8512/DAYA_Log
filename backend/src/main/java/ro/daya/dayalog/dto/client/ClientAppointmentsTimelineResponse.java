package ro.daya.dayalog.dto.client;

import java.util.List;
import java.util.UUID;

public record ClientAppointmentsTimelineResponse(
        UUID clientId,
        String clientFullName,
        List<ClientAppointmentTimelineItemResponse> futureAppointments,
        List<ClientAppointmentTimelineItemResponse> historyAppointments
) {
}