package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoInvalidQueryException extends TrinoErrorException {

    public TrinoInvalidQueryException(TrinoError trinoError) {
        super(trinoError, HttpStatus.BAD_REQUEST);
    }
}
