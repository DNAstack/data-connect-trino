package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.NonNull;

/**
 * Wraps whatever a handler failed with, to be answered to the caller by the global exception advice. The advice
 * reports it and turns the cause into a status and a
 * {@link com.dnastack.ga4gh.dataconnect.model.DataConnectErrorResponse}, which is the same body for every
 * endpoint - see that class for why one body serves them all.
 */
public class TableApiErrorException extends RuntimeException {

    public TableApiErrorException(@NonNull Throwable cause) {
        super(cause);
    }
}
