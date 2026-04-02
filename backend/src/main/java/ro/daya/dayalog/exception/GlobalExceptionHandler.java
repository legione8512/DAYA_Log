package ro.daya.dayalog.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ro.daya.dayalog.dto.common.ApiErrorResponse;
import ro.daya.dayalog.dto.common.ApiFieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex,
                                                               HttpServletRequest request) {
        return ApiErrorFactory.response(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getDetails(),
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ApiFieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return ApiErrorFactory.response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Datele trimise nu sunt valide.",
                request.getRequestURI(),
                null,
                fieldErrors
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Unul dintre parametrii trimiși are un format invalid.",
                request.getRequestURI(),
                null,
                List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Lipsește un parametru obligatoriu din cerere.",
                request.getRequestURI(),
                null,
                List.of(new ApiFieldError(ex.getParameterName(), "Parametrul este obligatoriu."))
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                 HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_CREDENTIALS",
                "Emailul sau parola sunt incorecte.",
                request.getRequestURI(),
                null,
                List.of()
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex,
                                                           HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_INACTIVE",
                "Contul de utilizator este inactiv.",
                request.getRequestURI(),
                null,
                List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex,
                                                          HttpServletRequest request) {
        return ApiErrorFactory.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "A apărut o eroare internă.",
                request.getRequestURI(),
                null,
                List.of()
        );
    }
}