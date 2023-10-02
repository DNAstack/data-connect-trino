package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoUserForbiddenException extends TrinoErrorException {
    public TrinoUserForbiddenException(TrinoError trinoError) {
        super(trinoError, HttpStatus.FORBIDDEN);
    }
}