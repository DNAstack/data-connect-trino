package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import lombok.Getter;

@Getter
public class TrinoErrorException extends RuntimeException {
    private final TrinoError trinoError;

    public TrinoErrorException(TrinoError trinoError) {
        this.trinoError = trinoError;
    }

    public TrinoErrorException(String message, TrinoError trinoError) {
        super(message);
        this.trinoError = trinoError;
    }

    public TrinoErrorException(String message, Throwable cause, TrinoError trinoError) {
        super(message, cause);
        this.trinoError = trinoError;
    }

    public TrinoErrorException(Throwable cause, TrinoError trinoError) {
        super(cause);
        this.trinoError = trinoError;
    }

    public String toString() {
        return trinoError.toString();
    }
}
