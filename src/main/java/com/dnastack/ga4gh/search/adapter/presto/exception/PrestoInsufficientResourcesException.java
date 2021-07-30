package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoInsufficientResourcesException extends PrestoErrorException {

    public PrestoInsufficientResourcesException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoInsufficientResourcesException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoInsufficientResourcesException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoInsufficientResourcesException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
