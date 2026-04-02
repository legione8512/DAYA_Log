package ro.daya.dayalog.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.EmailVerificationToken;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.enums.UserRole;
import ro.daya.dayalog.support.AbstractPostgresIntegrationTest;

class AuthControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void login_success_returnsAccessTokenAndRefreshCookie() throws Exception {
        AuthUserFixture fixture = createAuthUserFixture(UserRole.ADMIN, true, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Admin123!Change",
                                  "rememberMe": true
                                }
                                """.formatted(fixture.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andExpect(jsonPath("$.user.id").value(fixture.userId().toString()))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.email").value(fixture.email()))
                .andExpect(jsonPath("$.user.studioId").value(fixture.studioId().toString()))
                .andExpect(jsonPath("$.user.forcePasswordChange").value(true))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        AuthUserFixture fixture = createAuthUserFixture(UserRole.ADMIN, false, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "WrongPassword123!",
                                  "rememberMe": false
                                }
                                """.formatted(fixture.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void refresh_withValidCookie_returns200AndNewCookie() throws Exception {
        AuthUserFixture fixture = createAuthUserFixture(UserRole.ADMIN, false, false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Admin123!Change",
                                  "rememberMe": false
                                }
                                """.formatted(fixture.email())))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void logout_withValidCookie_returns204AndClearsCookie() throws Exception {
        AuthUserFixture fixture = createAuthUserFixture(UserRole.ADMIN, false, false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Admin123!Change",
                                  "rememberMe": false
                                }
                                """.formatted(fixture.email())))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void passwordResetConfirm_mismatch_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "dummy-token",
                                  "newPassword": "NewPassword123!",
                                  "confirmPassword": "DifferentPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_CONFIRM_MISMATCH"))
                .andExpect(jsonPath("$.path").value("/api/auth/password-reset/confirm"));
    }

    @Test
    void confirmEmail_success_marksUserVerified() throws Exception {
        AuthUserFixture fixture = createAuthUserFixture(UserRole.CLIENT, false, false);

        String rawToken = "email-confirm-token-" + UUID.randomUUID();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(entityManager.getReference(AppUser.class, fixture.userId()));
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        entityManager.persist(token);
        entityManager.flush();

        mockMvc.perform(get("/api/auth/confirm-email")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Adresa de email a fost confirmată cu succes."));

        entityManager.clear();

        AppUser updatedUser = entityManager.find(AppUser.class, fixture.userId());
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(updatedUser.getEmailVerified()));
    }

    private AuthUserFixture createAuthUserFixture(UserRole role,
                                                  boolean forcePasswordChange,
                                                  boolean emailVerified) {
        String suffix = UUID.randomUUID().toString().replace("-", "");

        Studio studio = new Studio();
        studio.setName("Auth Test Studio " + suffix);
        studio.setLegalName("Auth Test Studio " + suffix + " SRL");
        studio.setEmail("studio-auth+" + suffix + "@tests.local");
        studio.setPhone("+40" + suffix.substring(0, 8));
        studio.setAddressLine1("Auth Test Street 1");
        studio.setCity("Constanta");
        studio.setCounty("Constanta");
        studio.setPostcode("900001");
        studio.setActive(true);
        entityManager.persist(studio);

        AppUser user = new AppUser();
        user.setStudio(studio);
        user.setEmail("auth+" + suffix + "@tests.local");
        user.setPasswordHash(passwordEncoder.encode("Admin123!Change"));
        user.setRole(role);
        user.setEmailVerified(emailVerified);
        user.setForcePasswordChange(forcePasswordChange);
        user.setActive(true);
        entityManager.persist(user);

        entityManager.flush();

        return new AuthUserFixture(
                studio.getId(),
                user.getId(),
                user.getEmail()
        );
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash token", ex);
        }
    }

    private record AuthUserFixture(
            UUID studioId,
            UUID userId,
            String email
    ) {
    }
}