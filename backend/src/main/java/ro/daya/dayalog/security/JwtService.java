package ro.daya.dayalog.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey accessKey;
    private final Duration accessTokenLifetime = Duration.ofMinutes(15);

    public JwtService(@Value("${app.jwt.access-secret}") String accessSecret) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CurrentUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenLifetime);

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claims(Map.of(
                        "role", principal.getRole().name(),
                        "studioId", principal.getStudioId().toString(),
                        "email", principal.getUsername()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        return UUID.fromString(subject);
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public UUID extractStudioId(String token) {
        String studioId = extractClaim(token, claims -> claims.get("studioId", String.class));
        return UUID.fromString(studioId);
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        UUID userId = extractUserId(token);

        if (!(userDetails instanceof CurrentUserPrincipal principal)) {
            return false;
        }

        return userId.equals(principal.getId()) && !isTokenExpired(token);
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenLifetime.getSeconds();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}