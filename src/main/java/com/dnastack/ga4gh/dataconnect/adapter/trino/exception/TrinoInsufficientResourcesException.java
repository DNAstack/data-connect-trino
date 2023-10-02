package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoInsufficientResourcesException extends TrinoErrorException {

    public TrinoInsufficientResourcesException(TrinoError trinoError) {
        super(trinoError, HttpStatus.SERVICE_UNAVAILABLE);
    }

}
