package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

/**
 * The {@code GA4GH-Search-Authorization} header could not be read as a set of named credentials. Bad request
 * rather than forbidden: nothing has been judged about the caller's authority, because there is no credential
 * to judge.
 */
public class MalformedClientSuppliedCredentialsException extends ClientSuppliedCredentialsException {

    public MalformedClientSuppliedCredentialsException(String message) {
        super(message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
