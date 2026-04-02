package ro.daya.dayalog.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final HttpStatus status;
    private final String code;
    private final Object details;

    public BusinessRuleException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessRuleException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}