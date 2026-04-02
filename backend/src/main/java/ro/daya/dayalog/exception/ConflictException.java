package ro.daya.dayalog.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessRuleException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public ConflictException(String code, String message, Object details) {
        super(HttpStatus.CONFLICT, code, message, details);
    }
}