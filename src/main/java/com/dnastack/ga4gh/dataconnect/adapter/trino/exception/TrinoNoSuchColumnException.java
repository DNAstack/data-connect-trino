package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoNoSuchColumnException extends TrinoErrorException {

    public TrinoNoSuchColumnException(TrinoError trinoError) {
        super(trinoError, HttpStatus.BAD_REQUEST);
    }
}
