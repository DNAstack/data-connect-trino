package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

public class TrinoBadlyQualifiedNameException extends TrinoErrorException {

    public TrinoBadlyQualifiedNameException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
