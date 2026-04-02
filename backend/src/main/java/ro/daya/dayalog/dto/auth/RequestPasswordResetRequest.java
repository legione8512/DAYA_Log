package ro.daya.dayalog.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestPasswordResetRequest(
        @NotBlank @Email @Size(max = 150) String email
) {
}