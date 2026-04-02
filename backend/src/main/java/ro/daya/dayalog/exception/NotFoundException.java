package ro.daya.dayalog.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessRuleException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    public NotFoundException(String code, String message, Object details) {
        super(HttpStatus.NOT_FOUND, code, message, details);
    }
}