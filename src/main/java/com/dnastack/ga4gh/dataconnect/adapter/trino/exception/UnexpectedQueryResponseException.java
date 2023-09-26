package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

public class UnexpectedQueryResponseException extends RuntimeException implements HasHttpStatus {

    public UnexpectedQueryResponseException(String message) {
        super(message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
