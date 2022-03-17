package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;

public class TrinoUserForbiddenException extends TrinoErrorException {
    public TrinoUserForbiddenException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoUserForbiddenException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoUserForbiddenException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoUserForbiddenException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}