package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoUserUnauthorizedException extends TrinoErrorException {
    public TrinoUserUnauthorizedException(TrinoError trinoError) {
        super(trinoError, HttpStatus.UNAUTHORIZED);
    }
}