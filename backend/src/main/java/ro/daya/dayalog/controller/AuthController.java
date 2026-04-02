package ro.daya.dayalog.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.daya.dayalog.dto.auth.LoginRequest;
import ro.daya.dayalog.dto.auth.LoginResponse;
import ro.daya.dayalog.dto.auth.RefreshResponse;
import ro.daya.dayalog.dto.auth.RequestPasswordResetRequest;
import ro.daya.dayalog.dto.auth.ResetPasswordRequest;
import ro.daya.dayalog.dto.common.MessageResponse;
import ro.daya.dayalog.service.AuthService;
import ro.daya.dayalog.service.AuthService.LoginResult;
import ro.daya.dayalog.service.AuthService.RefreshResult;
import ro.daya.dayalog.exception.UnauthorizedException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final boolean secureCookies;

    public AuthController(AuthService authService,
                          @Value("${app.cookies.secure:false}") boolean secureCookies) {
        this.authService = authService;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        LoginResult result = authService.login(request);

        addRefreshCookie(response, result.rawRefreshToken(), result.refreshTokenMaxAgeSeconds());

        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(HttpServletRequest request,
                                                   HttpServletResponse response) {
        String rawRefreshToken = readRefreshCookie(request);

        RefreshResult result = authService.refresh(rawRefreshToken);

        addRefreshCookie(response, result.rawRefreshToken(), result.refreshTokenMaxAgeSeconds());

        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String rawRefreshToken = readRefreshCookie(request);

        authService.logout(rawRefreshToken);
        clearRefreshCookie(response);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public MessageResponse requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.confirmPasswordReset(request);
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<MessageResponse> confirmEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.confirmEmail(token));
    }
    

    private void addRefreshCookie(HttpServletResponse response,
                                  String rawRefreshToken,
                                  long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookies)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh token cookie lipsește.");
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh token cookie lipsește.");
    }
}