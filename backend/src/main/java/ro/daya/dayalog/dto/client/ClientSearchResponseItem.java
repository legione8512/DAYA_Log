package ro.daya.dayalog.dto.client;

import java.util.UUID;

public record ClientSearchResponseItem(
        UUID id,
        String fullName,
        String email,
        String phone,
        Boolean active,
        Boolean hasUserAccount
) {
}