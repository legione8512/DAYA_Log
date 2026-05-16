package ro.daya.dayalog.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetClientPasswordRequest(@NotBlank @Size(min = 10, max = 100) String newPassword,
		@NotBlank String confirmPassword, Boolean forcePasswordChange) {
}