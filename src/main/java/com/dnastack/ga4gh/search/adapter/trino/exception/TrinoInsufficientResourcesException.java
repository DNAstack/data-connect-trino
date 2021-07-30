package com.dnastack.ga4gh.search.adapter.trino.exception;

import com.dnastack.ga4gh.search.adapter.trino.TrinoError;

public class TrinoInsufficientResourcesException extends TrinoErrorException {

    public TrinoInsufficientResourcesException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoInsufficientResourcesException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoInsufficientResourcesException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoInsufficientResourcesException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
