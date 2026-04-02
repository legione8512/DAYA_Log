package ro.daya.dayalog.dto.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientDetailsResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String county,
        String postcode,
        LocalDate dateOfBirth,
        String gender,
        String leadSource,
        Boolean gdprConsent,
        Boolean emailAllowed,
        Boolean smsAllowed,
        Boolean marketingAllowed,
        String emergencyContactName,
        String emergencyContactPhone,
        String medicalNotes,
        String restrictions,
        Boolean active,
        Boolean hasUserAccount,
        String accountEmail,
        String accountRole,
        Boolean forcePasswordChange,
        List<ClientRecentAppointmentResponse> recentAppointments
) {
}