package ro.daya.dayalog.dto.auth;

import java.util.UUID;

import ro.daya.dayalog.entity.enums.UserRole;

public record CurrentUserResponse(
        UUID id,
        UserRole role,
        String email,
        UUID studioId,
        Boolean forcePasswordChange
) {
}