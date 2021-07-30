package com.dnastack.ga4gh.search.adapter.presto.exception;

public class UnexpectedQueryResponseException extends RuntimeException {

    public UnexpectedQueryResponseException() {
    }

    public UnexpectedQueryResponseException(String message) {
        super(message);
    }

    public UnexpectedQueryResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedQueryResponseException(Throwable cause) {
        super(cause);
    }
}
