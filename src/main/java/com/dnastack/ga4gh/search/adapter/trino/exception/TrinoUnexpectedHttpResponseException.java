package com.dnastack.ga4gh.search.adapter.trino.exception;

import lombok.Getter;

@Getter
public class TrinoUnexpectedHttpResponseException extends RuntimeException {
    private final int code;
    private final String logMessage;

    public TrinoUnexpectedHttpResponseException(int code, String message, String logMessage) {
        super(message);
        this.code = code;
        this.logMessage = logMessage;
    }

    public TrinoUnexpectedHttpResponseException(int code, String message, String logMessage, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.logMessage = logMessage;
    }
}
