package ro.daya.dayalog.dto.common;

public record ApiFieldError(
        String field,
        String message
) {
}