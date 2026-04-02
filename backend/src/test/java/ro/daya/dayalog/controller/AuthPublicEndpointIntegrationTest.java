package ro.daya.dayalog.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class AuthPublicEndpointIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void passwordResetRequestEndpoint_isPublic() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@example.com"
                                }
                                """))
                .andExpect(status().isOk());
    }
}