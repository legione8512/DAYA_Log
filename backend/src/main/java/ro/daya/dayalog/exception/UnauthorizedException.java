package ro.daya.dayalog.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessRuleException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public UnauthorizedException(String code, String message, Object details) {
        super(HttpStatus.UNAUTHORIZED, code, message, details);
    }
}