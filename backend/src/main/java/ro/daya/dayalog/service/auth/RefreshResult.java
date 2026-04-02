package ro.daya.dayalog.service.auth;

import ro.daya.dayalog.dto.auth.RefreshResponse;

public record RefreshResult(
        RefreshResponse response,
        String rawRefreshToken,
        long refreshTokenMaxAgeSeconds
) {
}