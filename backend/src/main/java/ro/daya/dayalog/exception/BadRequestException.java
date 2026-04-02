package ro.daya.dayalog.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }

    public BadRequestException(String code, String message, Object details) {
        super(HttpStatus.BAD_REQUEST, code, message, details);
    }
}