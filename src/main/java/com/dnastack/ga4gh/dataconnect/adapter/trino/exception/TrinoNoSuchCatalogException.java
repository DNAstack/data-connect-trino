package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
public class TrinoNoSuchCatalogException extends TrinoErrorException {

    public TrinoNoSuchCatalogException(TrinoError trinoError) {
        super(trinoError, HttpStatus.NOT_FOUND);
    }

    public TrinoNoSuchCatalogException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
