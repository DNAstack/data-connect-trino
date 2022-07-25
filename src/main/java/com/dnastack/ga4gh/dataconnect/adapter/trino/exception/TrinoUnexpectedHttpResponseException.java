package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.Getter;

@Getter
public class TrinoUnexpectedHttpResponseException extends RuntimeException {
    private final int code;

    public TrinoUnexpectedHttpResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public TrinoUnexpectedHttpResponseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
