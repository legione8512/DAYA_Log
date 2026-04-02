package ro.daya.dayalog.dto.appointment;

import jakarta.validation.constraints.NotBlank;

public record ChangeAppointmentStatusRequest(
        @NotBlank String status
) {
}