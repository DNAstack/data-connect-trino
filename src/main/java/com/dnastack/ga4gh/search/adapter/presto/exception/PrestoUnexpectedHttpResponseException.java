package com.dnastack.ga4gh.search.adapter.presto.exception;

import lombok.Getter;

import java.io.IOException;

@Getter
public class PrestoUnexpectedHttpResponseException extends RuntimeException {
    private final int code;
    private final String logMessage;

    public PrestoUnexpectedHttpResponseException(int code, String message, String logMessage) {
        super(message);
        this.code = code;
        this.logMessage = logMessage;
    }

    public PrestoUnexpectedHttpResponseException(int code, String message, String logMessage, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.logMessage = logMessage;
    }
}
