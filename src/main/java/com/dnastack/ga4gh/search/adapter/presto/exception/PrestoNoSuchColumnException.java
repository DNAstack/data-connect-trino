package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoNoSuchColumnException extends PrestoErrorException {

    public PrestoNoSuchColumnException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoNoSuchColumnException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoNoSuchColumnException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoNoSuchColumnException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
