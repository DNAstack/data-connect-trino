package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;
import lombok.Value;

@Value
public class PrestoNoSuchCatalogException extends PrestoErrorException {

    public PrestoNoSuchCatalogException(String message) {
        this(message, null);
    }

    public PrestoNoSuchCatalogException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoNoSuchCatalogException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoNoSuchCatalogException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoNoSuchCatalogException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
