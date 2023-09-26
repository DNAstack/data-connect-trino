package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoNoSuchSchemaException extends TrinoErrorException {

    public TrinoNoSuchSchemaException(TrinoError trinoError) {
        super(trinoError, HttpStatus.NOT_FOUND);
    }
}
