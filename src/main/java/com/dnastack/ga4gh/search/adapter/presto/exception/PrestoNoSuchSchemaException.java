package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;

public class PrestoNoSuchSchemaException extends PrestoErrorException {

    public PrestoNoSuchSchemaException(PrestoError prestoError) {
        super(prestoError);
    }

    public PrestoNoSuchSchemaException(String message, PrestoError prestoError) {
        super(message, prestoError);
    }

    public PrestoNoSuchSchemaException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause, prestoError);
    }

    public PrestoNoSuchSchemaException(Throwable cause, PrestoError prestoError) {
        super(cause, prestoError);
    }
}
