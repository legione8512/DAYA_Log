package ro.daya.dayalog.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 10, max = 100) String newPassword,
        @NotBlank String confirmPassword
) {
}