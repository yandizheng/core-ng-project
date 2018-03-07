package core.framework.web.exception;

import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.ResponseStatus;
import core.framework.log.ErrorCode;
import core.framework.log.Severity;

/**
 * @author neo
 */
@ResponseStatus(HTTPStatus.CONFLICT)
public final class ConflictException extends RuntimeException implements ErrorCode {
    public static final String DEFAULT_ERROR_CODE = "CONFLICT";

    private static final long serialVersionUID = 7787085179989898162L;

    private final String errorCode;

    public ConflictException(String message) {
        super(message);
        errorCode = DEFAULT_ERROR_CODE;
    }

    public ConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConflictException(String message, String errorCode, Throwable cause) {
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
