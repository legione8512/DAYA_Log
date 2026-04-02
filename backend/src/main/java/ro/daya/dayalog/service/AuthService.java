package ro.daya.dayalog.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.auth.CurrentUserResponse;
import ro.daya.dayalog.dto.auth.LoginRequest;
import ro.daya.dayalog.dto.auth.LoginResponse;
import ro.daya.dayalog.dto.auth.RefreshResponse;
import ro.daya.dayalog.dto.auth.RequestPasswordResetRequest;
import ro.daya.dayalog.dto.auth.ResetPasswordRequest;
import ro.daya.dayalog.dto.common.MessageResponse;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.EmailVerificationToken;
import ro.daya.dayalog.entity.PasswordResetToken;
import ro.daya.dayalog.entity.RefreshToken;
import ro.daya.dayalog.repository.AppUserRepository;
import ro.daya.dayalog.repository.EmailVerificationTokenRepository;
import ro.daya.dayalog.repository.PasswordResetTokenRepository;
import ro.daya.dayalog.repository.RefreshTokenRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.security.JwtService;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.UnauthorizedException;
import ro.daya.dayalog.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Duration REFRESH_TOKEN_LIFETIME_DEFAULT = Duration.ofDays(1);
    private static final Duration REFRESH_TOKEN_LIFETIME_REMEMBER_ME = Duration.ofDays(30);
    private static final Duration PASSWORD_RESET_TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration EMAIL_VERIFICATION_TOKEN_LIFETIME = Duration.ofHours(24);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String appBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppUserRepository appUserRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AuditLogService auditLogService,
                       @Value("${app.base-url}") String appBaseUrl) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim(),
                        request.password()
                )
        );

        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();

        AppUser user = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        user.setLastLoginAt(OffsetDateTime.now());

        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        Duration refreshLifetime = rememberMe
                ? REFRESH_TOKEN_LIFETIME_REMEMBER_ME
                : REFRESH_TOKEN_LIFETIME_DEFAULT;

        String accessToken = jwtService.generateAccessToken(principal);

        String rawRefreshToken = generateRawToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setRememberMe(rememberMe);
        refreshToken.setExpiresAt(OffsetDateTime.now().plus(refreshLifetime));

        refreshTokenRepository.save(refreshToken);

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                toCurrentUserResponse(principal)
        );

        return new LoginResult(response, rawRefreshToken, refreshLifetime.getSeconds());
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh tokenul lipsește.");
        }

        String refreshTokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findActiveTokenByHash(refreshTokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "REFRESH_TOKEN_INVALID",
                        "Refresh tokenul este invalid sau a expirat."
                ));

        AppUser user = storedToken.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("ACCOUNT_INACTIVE", "Contul de utilizator este inactiv.");
        }

        boolean rememberMe = Boolean.TRUE.equals(storedToken.getRememberMe());
        Duration refreshLifetime = rememberMe
                ? REFRESH_TOKEN_LIFETIME_REMEMBER_ME
                : REFRESH_TOKEN_LIFETIME_DEFAULT;

        refreshTokenRepository.revokeById(storedToken.getId(), OffsetDateTime.now());

        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                user.getId(),
                user.getStudio().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getForcePasswordChange(),
                user.getActive()
        );

        String accessToken = jwtService.generateAccessToken(principal);

        String newRawRefreshToken = generateRawToken();
        String newRefreshTokenHash = hashToken(newRawRefreshToken);

        RefreshToken newStoredToken = new RefreshToken();
        newStoredToken.setUser(user);
        newStoredToken.setTokenHash(newRefreshTokenHash);
        newStoredToken.setRememberMe(rememberMe);
        newStoredToken.setExpiresAt(OffsetDateTime.now().plus(refreshLifetime));
        newStoredToken.setLastUsedAt(OffsetDateTime.now());

        refreshTokenRepository.save(newStoredToken);

        RefreshResponse response = new RefreshResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds()
        );

        return new RefreshResult(response, newRawRefreshToken, refreshLifetime.getSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String refreshTokenHash = hashToken(rawRefreshToken);

        refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(refreshTokenHash)
                .ifPresent(token -> refreshTokenRepository.revokeById(token.getId(), OffsetDateTime.now()));
    }

    @Transactional
    public MessageResponse requestPasswordReset(RequestPasswordResetRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(this::createAndSendPasswordResetToken);

        return new MessageResponse(
                "Dacă există un cont pentru această adresă de email, vei primi în curând instrucțiuni pentru resetarea parolei."
        );
    }

    @Transactional
    public MessageResponse confirmPasswordReset(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
        	throw new BadRequestException(
        	        "PASSWORD_RESET_CONFIRM_MISMATCH",
        	        "Confirmarea parolei nu corespunde."
        	);        }

        validatePasswordStrength(request.newPassword());

        String tokenHash = hashToken(request.token());
        PasswordResetToken storedToken = passwordResetTokenRepository
                .findActiveTokenByHash(tokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "PASSWORD_RESET_TOKEN_INVALID",
                        "Tokenul de resetare este invalid sau a expirat."
                ));

        AppUser user = storedToken.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ConflictException("ACCOUNT_INACTIVE", "Contul de utilizator este inactiv.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);

        passwordResetTokenRepository.markUsed(storedToken.getId(), OffsetDateTime.now());
        refreshTokenRepository.revokeAllActiveTokensForUser(user.getId(), OffsetDateTime.now());

        auditLogService.log(
                user.getStudio().getId(),
                user.getId(),
                "app_user",
                user.getId(),
                "PASSWORD_RESET_CONFIRM",
                passwordResetConfirmSummary(user)
        );

        return new MessageResponse("Parola a fost resetată cu succes.");
    }

    @Transactional
    public MessageResponse confirmEmail(String rawToken) {
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken storedToken = emailVerificationTokenRepository
                .findActiveTokenByHash(tokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "EMAIL_CONFIRMATION_TOKEN_INVALID",
                        "Tokenul de confirmare email este invalid sau a expirat."
                ));

        AppUser user = storedToken.getUser();

        if (user == null) {
            throw new UnauthorizedException(
                    "EMAIL_CONFIRMATION_TOKEN_INVALID",
                    "Tokenul de confirmare email este invalid."
            );
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ConflictException(
                    "EMAIL_ALREADY_CONFIRMED",
                    "Adresa de email este deja confirmată."
            );
        }

        user.setEmailVerified(true);

        appUserRepository.saveAndFlush(user);
        emailVerificationTokenRepository.markUsed(storedToken.getId(), OffsetDateTime.now());

        auditLogService.log(
                user.getStudio().getId(),
                user.getId(),
                "app_user",
                user.getId(),
                "EMAIL_CONFIRM",
                emailConfirmSummary(user)
        );

        return new MessageResponse("Adresa de email a fost confirmat\u0103 cu succes.");
    }

    @Transactional
    public void sendEmailConfirmation(AppUser user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        emailVerificationTokenRepository.deleteUnusedTokensForUser(user.getId());

        String rawToken = generateRawToken();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(OffsetDateTime.now().plus(EMAIL_VERIFICATION_TOKEN_LIFETIME));

        emailVerificationTokenRepository.save(token);

        String confirmationLink = buildPublicLink("/confirm-email", rawToken);
        String subject = "Confirmă adresa de email - DAYA Log";
        String body = "Bună,\n\n"
                + "Te rugăm să confirmi adresa de email folosind linkul de mai jos:\n"
                + confirmationLink + "\n\n"
                + "Linkul expiră în 24 de ore.\n\n"
                + "Echipa DAYA Log";

        emailService.send(user.getEmail(), subject, body);
    }

    private void createAndSendPasswordResetToken(AppUser user) {
        passwordResetTokenRepository.deleteUnusedTokensForUser(user.getId());

        String rawToken = generateRawToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(OffsetDateTime.now().plus(PASSWORD_RESET_TOKEN_LIFETIME));

        passwordResetTokenRepository.save(token);

        String resetLink = buildPublicLink("/reset-password", rawToken);
        String subject = "Resetare parolă - DAYA Log";
        String body = "Bună,\n\n"
                + "Am primit o cerere de resetare a parolei pentru contul tău.\n"
                + "Folosește linkul de mai jos pentru a seta o parolă nouă:\n"
                + resetLink + "\n\n"
                + "Linkul expiră în 1 oră. Dacă nu ai cerut resetarea, poți ignora acest email.\n\n"
                + "Echipa DAYA Log";

        emailService.send(user.getEmail(), subject, body);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 10) {
        	throw new BadRequestException(
        	        "PASSWORD_TOO_SHORT",
        	        "Parola trebuie să conțină minimum 10 caractere."
        	);        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
        	throw new BadRequestException(
        	        "PASSWORD_COMPLEXITY_INVALID",
        	        "Parola trebuie să conțină cel puțin o literă mare, o literă mică, o cifră și un caracter special."
        	);
        }
    }

    private String buildPublicLink(String path, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return appBaseUrl + path + "?token=" + encodedToken;
    }

    private CurrentUserResponse toCurrentUserResponse(CurrentUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getRole(),
                principal.getUsername(),
                principal.getStudioId(),
                principal.getForcePasswordChange()
        );
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
    private Map<String, Object> passwordResetConfirmSummary(AppUser user) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("email", user.getEmail());
        summary.put("forcePasswordChange", user.getForcePasswordChange());
        summary.put("active", user.getActive());
        summary.put("emailVerified", user.getEmailVerified());
        summary.put("allRefreshTokensRevoked", true);
        return summary;
    }

    private Map<String, Object> emailConfirmSummary(AppUser user) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("email", user.getEmail());
        summary.put("emailVerified", user.getEmailVerified());
        summary.put("active", user.getActive());
        return summary;
    }

    public record LoginResult(
            LoginResponse response,
            String rawRefreshToken,
            long refreshTokenMaxAgeSeconds
    ) {}

    public record RefreshResult(
            RefreshResponse response,
            String rawRefreshToken,
            long refreshTokenMaxAgeSeconds
    ) {}
}