package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoInvalidQueryException extends PrestoErrorException {

    public PrestoInvalidQueryException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoInvalidQueryException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoInvalidQueryException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoInvalidQueryException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
