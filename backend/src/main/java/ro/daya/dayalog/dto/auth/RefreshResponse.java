package ro.daya.dayalog.dto.auth;

public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}