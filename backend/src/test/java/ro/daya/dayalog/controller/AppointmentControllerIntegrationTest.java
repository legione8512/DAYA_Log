package ro.daya.dayalog.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;
import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class AppointmentControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void createIndividualAppointment_success() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        OffsetDateTime startAt = OffsetDateTime.parse("2030-06-10T10:00:00+03:00");
        OffsetDateTime endAt = OffsetDateTime.parse("2030-06-10T11:00:00+03:00");

        mockMvc.perform(post("/api/admin/appointments")
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content(asJson(appointmentPayload(
                                "INDIVIDUAL",
                                fixture.serviceId(),
                                fixture.primaryInstructorId(),
                                startAt,
                                endAt,
                                List.of(fixture.clientOneId()),
                                1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.serviceId").value(fixture.serviceId().toString()))
                .andExpect(jsonPath("$.instructorId").value(fixture.primaryInstructorId().toString()))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].clientId").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.participants[0].participationStatus").value("BOOKED"));
    }

    @Test
    void createAppointment_rejectsOverlapForSameClient() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        createAppointment(
                fixture,
                fixture.primaryInstructorId(),
                OffsetDateTime.parse("2030-06-11T10:00:00+03:00"),
                OffsetDateTime.parse("2030-06-11T11:00:00+03:00"),
                AppointmentType.INDIVIDUAL,
                AppointmentStatus.SCHEDULED,
                1,
                List.of(fixture.clientOneId())
        );

        mockMvc.perform(post("/api/admin/appointments")
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content(asJson(appointmentPayload(
                                "INDIVIDUAL",
                                fixture.serviceId(),
                                fixture.secondaryInstructorId(),
                                OffsetDateTime.parse("2030-06-11T10:30:00+03:00"),
                                OffsetDateTime.parse("2030-06-11T11:30:00+03:00"),
                                List.of(fixture.clientOneId()),
                                1
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_CLIENT_CONFLICT"));
    }

    @Test
    void addParticipantToExistingGroup_success() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        UUID appointmentId = createAppointment(
                fixture,
                fixture.primaryInstructorId(),
                OffsetDateTime.parse("2030-06-15T10:00:00+03:00"),
                OffsetDateTime.parse("2030-06-15T11:00:00+03:00"),
                AppointmentType.GROUP,
                AppointmentStatus.SCHEDULED,
                3,
                List.of(fixture.clientOneId())
        );

        mockMvc.perform(post("/api/admin/appointments/{id}/add-participants", appointmentId)
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "participantClientIds", List.of(fixture.clientTwoId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$..clientId", hasItem(fixture.clientTwoId().toString())));
    }

    @Test
    void addParticipant_rejectsDuplicateParticipant() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        UUID appointmentId = createAppointment(
                fixture,
                fixture.primaryInstructorId(),
                OffsetDateTime.parse("2030-06-16T10:00:00+03:00"),
                OffsetDateTime.parse("2030-06-16T11:00:00+03:00"),
                AppointmentType.GROUP,
                AppointmentStatus.SCHEDULED,
                3,
                List.of(fixture.clientOneId())
        );

        mockMvc.perform(post("/api/admin/appointments/{id}/add-participants", appointmentId)
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "participantClientIds", List.of(fixture.clientOneId())
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_PARTICIPANT_ALREADY_EXISTS"));
    }

    private Map<String, Object> appointmentPayload(String appointmentType,
                                                   UUID serviceId,
                                                   UUID instructorId,
                                                   OffsetDateTime startAt,
                                                   OffsetDateTime endAt,
                                                   List<UUID> participantClientIds,
                                                   int capacity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentType", appointmentType);
        payload.put("serviceId", serviceId);
        payload.put("instructorId", instructorId);
        payload.put("startAt", startAt);
        payload.put("endAt", endAt);
        payload.put("status", "SCHEDULED");
        payload.put("participantClientIds", participantClientIds);
        payload.put("capacity", capacity);
        return payload;
    }
}