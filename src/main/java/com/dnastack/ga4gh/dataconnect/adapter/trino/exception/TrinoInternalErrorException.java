package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;

public class TrinoInternalErrorException extends TrinoErrorException {

    public TrinoInternalErrorException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoInternalErrorException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoInternalErrorException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoInternalErrorException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
