package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TrinoUnexpectedHttpResponseException extends RuntimeException implements HasHttpStatus {
    private final int code;

    public TrinoUnexpectedHttpResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public TrinoUnexpectedHttpResponseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
