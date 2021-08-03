package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

public class TrinoBadlyQualifiedNameException extends RuntimeException {

    public TrinoBadlyQualifiedNameException() {
    }

    public TrinoBadlyQualifiedNameException(String message) {
        super(message);
    }

    public TrinoBadlyQualifiedNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrinoBadlyQualifiedNameException(Throwable cause) {
        super(cause);
    }
}
