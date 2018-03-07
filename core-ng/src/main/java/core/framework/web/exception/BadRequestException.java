package core.framework.web.exception;

import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.ResponseStatus;
import core.framework.log.ErrorCode;
import core.framework.log.Severity;

/**
 * @author neo
 */
@ResponseStatus(HTTPStatus.BAD_REQUEST)
public final class BadRequestException extends RuntimeException implements ErrorCode {
    public static final String DEFAULT_ERROR_CODE = "BAD_REQUEST";

    private static final long serialVersionUID = -2304226404736886782L;

    private final String errorCode;

    public BadRequestException(String message) {
        super(message);
        errorCode = DEFAULT_ERROR_CODE;
    }

    public BadRequestException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BadRequestException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public Severity severity() {
        return Severity.WARN;
    }

    @Override
    public String errorCode() {
        return errorCode;
    }
}
