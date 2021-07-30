package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoInternalErrorException extends PrestoErrorException {

    public PrestoInternalErrorException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoInternalErrorException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoInternalErrorException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoInternalErrorException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
