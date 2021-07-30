package com.dnastack.ga4gh.search.adapter.trino.exception;

import com.dnastack.ga4gh.search.adapter.trino.TrinoError;

public class TrinoNoSuchColumnException extends TrinoErrorException {

    public TrinoNoSuchColumnException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoNoSuchColumnException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoNoSuchColumnException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoNoSuchColumnException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
