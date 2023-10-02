package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TrinoErrorException extends RuntimeException implements HasHttpStatus {

    private final HttpStatus httpStatus;

    /**
     * Constructs an exception representing the given Trino error.
     * @param trinoError the Trino error message we received from our call to Trino.
     * @param httpStatus the HTTP status code that best categorizes this exception from the point of view of a caller
     *                   into this microservice. This is <b>not necessarily</b> the HTTP status we got from our call to Trino.
     */
    public TrinoErrorException(TrinoError trinoError, HttpStatus httpStatus) {
        super(String.format(
                "%s: %s: %s",
                trinoError.getErrorType(),
                trinoError.getErrorCode(),
                trinoError.getErrorName()
        ));
        this.httpStatus = httpStatus;
    }

    /**
     * Constructs an exception with the given message. Use this only when a TrinoError object isn't available.
     * @param message a description of the error. Expect this to be relayed back to external users of the Data Connect API.
     * @param httpStatus the HTTP status code that best categorizes this exception from the point of view of a caller
     *                   into this microservice. This is <b>not necessarily</b> the HTTP status we got from our call to Trino.
     */
    public TrinoErrorException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
