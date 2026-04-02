package ro.daya.dayalog.service.auth;

import ro.daya.dayalog.dto.auth.LoginResponse;

public record LoginResult(
        LoginResponse response,
        String rawRefreshToken,
        long refreshTokenMaxAgeSeconds
) {
}