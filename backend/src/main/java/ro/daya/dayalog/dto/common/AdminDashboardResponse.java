package ro.daya.dayalog.dto.common;

public record AdminDashboardResponse(
        long activeClientsCount,
        long todayAppointmentsCount,
        long upcomingAppointmentsCount,
        long activeServicesCount,
        long activeInstructorsCount,
        long activeResourcesCount
) {
}