package ro.daya.dayalog.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class ProfileControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void profileMe_unauthenticated_returns401Json() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/profile/me"));
    }

    @Test
    void profileMe_authenticated_returnsCurrentUser() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(get("/api/profile/me")
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.adminUserId().toString()))
                .andExpect(jsonPath("$.email").value(fixture.adminEmail()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.studioId").value(fixture.studioId().toString()));
    }

    @Test
    void changePassword_unauthenticated_returns401Json() throws Exception {
        mockMvc.perform(post("/api/profile/change-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Admin123!Change",
                                  "newPassword": "Admin456!Change",
                                  "confirmNewPassword": "Admin456!Change"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/profile/change-password"));
    }

    @Test
    void changePassword_success_oldPasswordStopsWorking_newPasswordWorks() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(post("/api/profile/change-password")
                        .with(adminAuth(fixture))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Admin123!Change",
                                  "newPassword": "Admin456!Change",
                                  "confirmNewPassword": "Admin456!Change"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Admin123!Change",
                                  "rememberMe": false
                                }
                                """.formatted(fixture.adminEmail())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Admin456!Change",
                                  "rememberMe": false
                                }
                                """.formatted(fixture.adminEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}