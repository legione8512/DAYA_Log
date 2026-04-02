package ro.daya.dayalog.dto.appointment;

import java.util.List;

public record AppointmentFormOptionsResponse(
        List<ServiceOptionResponse> services,
        List<InstructorOptionResponse> instructors,
        List<ResourceOptionResponse> resources
) {
}