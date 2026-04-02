package ro.daya.dayalog.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientUserAccountRequest(
        @Email @NotBlank @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String initialPassword,
        Boolean forcePasswordChange
) {
}