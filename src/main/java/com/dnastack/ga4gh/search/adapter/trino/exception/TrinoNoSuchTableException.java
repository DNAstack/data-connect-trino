package com.dnastack.ga4gh.search.adapter.trino.exception;

import com.dnastack.ga4gh.search.adapter.trino.TrinoError;

public class TrinoNoSuchTableException extends TrinoErrorException {

    public TrinoNoSuchTableException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoNoSuchTableException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoNoSuchTableException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoNoSuchTableException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
