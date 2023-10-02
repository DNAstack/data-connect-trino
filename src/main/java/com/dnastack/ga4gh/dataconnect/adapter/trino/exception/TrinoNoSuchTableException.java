package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

public class TrinoNoSuchTableException extends TrinoErrorException {

    public TrinoNoSuchTableException(TrinoError trinoError) {
        super(trinoError, HttpStatus.NOT_FOUND);
    }
}
