package ro.daya.dayalog.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Object details,
        List<ApiFieldError> fieldErrors
) {
}