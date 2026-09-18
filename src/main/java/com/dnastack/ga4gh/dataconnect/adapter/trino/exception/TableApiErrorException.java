package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.NonNull;

/**
 * General, uniform exception for errors that come from the Data Connect implementation. There is a global
 * ControllerAdvice that logs all exceptions of this type and turns the cause into an HTTP response status and a
 * {@link com.dnastack.ga4gh.dataconnect.model.DataConnectErrorResponse} body.
 */
public class TableApiErrorException extends RuntimeException {

    public TableApiErrorException(@NonNull Throwable cause) {
        super(cause);
    }
}
