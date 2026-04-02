package ro.daya.dayalog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class SecurityAccessIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void unauthenticatedRequest_toAdminEndpoint_returns401Json() throws Exception {
        mockMvc.perform(get("/api/admin/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/admin/clients"));
    }

    @Test
    void clientCannotAccessAdminEndpoint_returns403Json() throws Exception {
        ClientSelfFixture fixture = createClientSelfFixture();

        mockMvc.perform(get("/api/admin/clients")
                        .with(clientAuth(fixture)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/admin/clients"));
    }

    @Test
    void adminCanAccessAdminClientsEndpoint_returns200() throws Exception {
        AppointmentFixture fixture = createAppointmentFixture();

        mockMvc.perform(get("/api/admin/clients")
                        .with(adminAuth(fixture)))
                .andExpect(status().isOk());
    }

    @Test
    void clientCanAccessOwnFutureAppointments_returns200() throws Exception {
        ClientSelfFixture fixture = createClientSelfFixture();

        mockMvc.perform(get("/api/client/appointments/future")
                        .with(clientAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void clientCanAccessOwnHistoryAppointments_returns200() throws Exception {
        ClientSelfFixture fixture = createClientSelfFixture();

        mockMvc.perform(get("/api/client/appointments/history")
                        .with(clientAuth(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}