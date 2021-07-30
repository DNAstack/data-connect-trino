package com.dnastack.ga4gh.search.adapter.presto.exception;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;
import lombok.Getter;

@Getter
public class PrestoErrorException extends RuntimeException {
    private final PrestoError prestoError;

    public PrestoErrorException(PrestoError prestoError) {
        this.prestoError = prestoError;
    }

    public PrestoErrorException(String message, PrestoError prestoError) {
        super(message);
        this.prestoError = prestoError;
    }

    public PrestoErrorException(String message, Throwable cause, PrestoError prestoError) {
        super(message, cause);
        this.prestoError = prestoError;
    }

    public PrestoErrorException(Throwable cause, PrestoError prestoError) {
        super(cause);
        this.prestoError = prestoError;
    }

    public String toString() {
        return prestoError.toString();
    }
}
