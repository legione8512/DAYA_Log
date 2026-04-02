package ro.daya.dayalog.exception;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.ObjectMapper;

import ro.daya.dayalog.dto.common.ApiErrorResponse;
import ro.daya.dayalog.dto.common.ApiFieldError;

public final class ApiErrorFactory {

    private ApiErrorFactory() {
    }

    public static ApiErrorResponse build(HttpStatus status,
                                         String code,
                                         String message,
                                         String path,
                                         Object details,
                                         List<ApiFieldError> fieldErrors) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details,
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors)
        );
    }

    public static ResponseEntity<ApiErrorResponse> response(HttpStatus status,
                                                            String code,
                                                            String message,
                                                            String path,
                                                            Object details,
                                                            List<ApiFieldError> fieldErrors) {
        return ResponseEntity.status(status)
                .body(build(status, code, message, path, details, fieldErrors));
    }

    public static void write(HttpServletResponse response,
                             ObjectMapper objectMapper,
                             HttpStatus status,
                             String code,
                             String message,
                             String path,
                             Object details,
                             List<ApiFieldError> fieldErrors) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                build(status, code, message, path, details, fieldErrors)
        );
    }
}