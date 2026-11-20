package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import org.springframework.http.HttpStatus;

/**
 * Trino failed the query with {@link #TRINO_ERROR_NAME}: a service Trino called on the query's behalf, such as
 * collection-service, doesn't know the request's tenant. This service does know it, so the tenant exists and the other
 * service hasn't learned of it yet. That is temporary, hence 503 rather than 404.
 */
public class TrinoUnknownTenantException extends TrinoErrorException {

    /** The name of the error code DNAstack's Trino plugins raise for an unknown tenant. */
    public static final String TRINO_ERROR_NAME = "UNKNOWN_TENANT";

    public TrinoUnknownTenantException(TrinoError trinoError) {
        super(trinoError, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
