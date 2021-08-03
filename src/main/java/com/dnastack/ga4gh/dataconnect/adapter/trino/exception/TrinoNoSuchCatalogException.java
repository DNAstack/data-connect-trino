package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import lombok.Value;

@Value
public class TrinoNoSuchCatalogException extends TrinoErrorException {

    public TrinoNoSuchCatalogException(String message) {
        this(message, null);
    }

    public TrinoNoSuchCatalogException(TrinoError trinoError) {
        super(trinoError);
    }

    public TrinoNoSuchCatalogException(String message, TrinoError trinoError) {
        super(message, trinoError);
    }

    public TrinoNoSuchCatalogException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause, trinoError);
    }

    public TrinoNoSuchCatalogException(Throwable cause, TrinoError trinoError) {
        super(cause, trinoError);
    }
}
