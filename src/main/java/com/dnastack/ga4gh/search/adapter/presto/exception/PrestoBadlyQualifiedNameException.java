package com.dnastack.ga4gh.search.adapter.presto.exception;

public class PrestoBadlyQualifiedNameException extends RuntimeException {

    public PrestoBadlyQualifiedNameException() {
    }

    public PrestoBadlyQualifiedNameException(String message) {
        super(message);
    }

    public PrestoBadlyQualifiedNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrestoBadlyQualifiedNameException(Throwable cause) {
        super(cause);
    }
}
