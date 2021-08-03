package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;

public class TrinoNoSuchSchemaException extends TrinoErrorException {

    public TrinoNoSuchSchemaException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoNoSuchSchemaException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoNoSuchSchemaException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoNoSuchSchemaException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
