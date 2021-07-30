package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoNoSuchTableException extends PrestoErrorException {

    public PrestoNoSuchTableException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoNoSuchTableException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoNoSuchTableException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoNoSuchTableException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
