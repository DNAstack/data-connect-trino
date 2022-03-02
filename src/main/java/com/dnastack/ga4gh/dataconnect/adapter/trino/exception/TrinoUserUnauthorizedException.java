package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;

public class TrinoUserUnauthorizedException extends TrinoErrorException {
    public TrinoUserUnauthorizedException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoUserUnauthorizedException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoUserUnauthorizedException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoUserUnauthorizedException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}