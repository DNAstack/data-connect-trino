package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;

public class TrinoInvalidQueryException extends TrinoErrorException {

    public TrinoInvalidQueryException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoInvalidQueryException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoInvalidQueryException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoInvalidQueryException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
