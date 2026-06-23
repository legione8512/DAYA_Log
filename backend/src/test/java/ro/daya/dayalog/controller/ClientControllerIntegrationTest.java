package ro.daya.dayalog.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;
import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class ClientControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void search_returnsMatchingClientsForAdmin() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(get("/api/admin/clients")
                        .with(adminAuth(fixture))
                        .param("query", "Elena")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$[0].fullName").value("Elena Marin"))
                .andExpect(jsonPath("$[0].email", containsString("elena+")))
                .andExpect(jsonPath("$[0].email", containsString("@tests.local")))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].hasUserAccount").value(false));
    }

    @Test
    void getById_returnsClientDetails() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(get("/api/admin/clients/{id}", fixture.clientOneId())
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.firstName").value("Elena"))
                .andExpect(jsonPath("$.lastName").value("Marin"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasUserAccount").value(false))
                .andExpect(jsonPath("$.recentAppointments.length()").value(0));
    }

    @Test
    void create_createsClientSuccessfully() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/admin/clients")
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Bianca",
                                  "lastName": "Stan",
                                  "email": "BIANCA.%s@tests.local",
                                  "phone": "+40711%s",
                                  "addressLine1": "Street 10",
                                  "city": "Constanta",
                                  "county": "Constanta",
                                  "postcode": "900100",
                                  "gender": "female",
                                  "leadSource": "Instagram",
                                  "gdprConsent": true,
                                  "emailAllowed": true,
                                  "smsAllowed": false,
                                  "marketingAllowed": true,
                                  "medicalNotes": "No issues",
                                  "restrictions": "None"
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Bianca"))
                .andExpect(jsonPath("$.lastName").value("Stan"))
                .andExpect(jsonPath("$.email").value(("bianca." + suffix + "@tests.local").toLowerCase()))
                .andExpect(jsonPath("$.phone").value("+40711" + suffix))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasUserAccount").value(false));
    }

    @Test
    void create_withDuplicatePhone_returns409() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        MvcResult existingClientResult = mockMvc.perform(get("/api/admin/clients/{id}", fixture.clientOneId())
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode existingClientJson = new ObjectMapper()
                .readTree(existingClientResult.getResponse().getContentAsString());

        String existingPhone = existingClientJson.get("phone").asText();

        mockMvc.perform(post("/api/admin/clients")
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Another",
                                  "lastName": "Client",
                                  "email": "another.client@tests.local",
                                  "phone": "%s",
                                  "gdprConsent": true,
                                  "emailAllowed": true,
                                  "smsAllowed": false,
                                  "marketingAllowed": false
                                }
                                """.formatted(existingPhone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CLIENT_PHONE_CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/clients"));
    }

    @Test
    void update_updatesClientSuccessfully() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(put("/api/admin/clients/{id}", fixture.clientOneId())
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Elena Updated",
                                  "lastName": "Marin Updated",
                                  "email": "ELENA.UPDATED@tests.local",
                                  "phone": "+40700111222",
                                  "addressLine1": "Updated Street 22",
                                  "addressLine2": "Flat 4",
                                  "city": "Bucharest",
                                  "county": "Bucharest",
                                  "postcode": "010101",
                                  "gender": "other",
                                  "leadSource": "Referral",
                                  "gdprConsent": true,
                                  "emailAllowed": true,
                                  "smsAllowed": true,
                                  "marketingAllowed": false,
                                  "emergencyContactName": "Maria Marin",
                                  "emergencyContactPhone": "+40744111222",
                                  "medicalNotes": "Updated notes",
                                  "restrictions": "Updated restrictions"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.firstName").value("Elena Updated"))
                .andExpect(jsonPath("$.lastName").value("Marin Updated"))
                .andExpect(jsonPath("$.email").value("elena.updated@tests.local"))
                .andExpect(jsonPath("$.phone").value("+40700111222"))
                .andExpect(jsonPath("$.city").value("Bucharest"))
                .andExpect(jsonPath("$.gender").value("OTHER"))
                .andExpect(jsonPath("$.smsAllowed").value(true));
    }

    @Test
    void updateStatus_deactivatesClientSuccessfully() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(patch("/api/admin/clients/{id}/status", fixture.clientOneId())
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/clients/{id}", fixture.clientOneId())
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void getAppointmentsTimeline_returnsFutureAndHistoryAppointments() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        OffsetDateTime now = OffsetDateTime.now();

        UUID historyAppointmentId = createAppointment(
                fixture,
                fixture.primaryInstructorId(),
                now.minusDays(3),
                now.minusDays(3).plusHours(1),
                AppointmentType.INDIVIDUAL,
                AppointmentStatus.COMPLETED,
                1,
                List.of(fixture.clientOneId())
        );

        UUID futureAppointmentId = createAppointment(
                fixture,
                fixture.secondaryInstructorId(),
                now.plusDays(3),
                now.plusDays(3).plusHours(1),
                AppointmentType.INDIVIDUAL,
                AppointmentStatus.CONFIRMED,
                1,
                List.of(fixture.clientOneId())
        );

        mockMvc.perform(get("/api/admin/clients/{id}/appointments", fixture.clientOneId())
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.clientFullName").value("Elena Marin"))
                .andExpect(jsonPath("$.futureAppointments.length()").value(1))
                .andExpect(jsonPath("$.historyAppointments.length()").value(1))
                .andExpect(jsonPath("$.futureAppointments[0].id").value(futureAppointmentId.toString()))
                .andExpect(jsonPath("$.historyAppointments[0].id").value(historyAppointmentId.toString()))
                .andExpect(jsonPath("$.futureAppointments[0].appointmentType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.historyAppointments[0].status").value("COMPLETED"));
    }

    @Test
    void createUserAccount_linksClientToNewUserAccount() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(post("/api/admin/clients/{id}/create-user-account", fixture.clientOneId())
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client.account@tests.local",
                                  "initialPassword": "Client123!Change",
                                  "forcePasswordChange": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.clientOneId().toString()))
                .andExpect(jsonPath("$.hasUserAccount").value(true))
                .andExpect(jsonPath("$.accountEmail").value("client.account@tests.local"))
                .andExpect(jsonPath("$.accountRole").value("CLIENT"))
                .andExpect(jsonPath("$.forcePasswordChange").value(true));
    }
}