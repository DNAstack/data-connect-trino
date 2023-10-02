package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoInternalErrorException extends TrinoErrorException {

    public TrinoInternalErrorException(TrinoError trinoError) {
        super(trinoError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
