package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

public class QueryParsingException extends RuntimeException implements HasHttpStatus {

    public QueryParsingException(String message) {
        super(message);
    }

    public QueryParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
