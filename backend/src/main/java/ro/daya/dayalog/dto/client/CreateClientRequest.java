package ro.daya.dayalog.dto.client;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String phone,
        @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String county,
        @Size(max = 20) String postcode,
        LocalDate dateOfBirth,
        String gender,
        @Size(max = 100) String leadSource,
        Boolean gdprConsent,
        Boolean emailAllowed,
        Boolean smsAllowed,
        Boolean marketingAllowed,
        @Size(max = 150) String emergencyContactName,
        @Size(max = 30) String emergencyContactPhone,
        String medicalNotes,
        String restrictions,
        Boolean createUserAccount,
        String initialPassword,
        Boolean forcePasswordChange
) {
}