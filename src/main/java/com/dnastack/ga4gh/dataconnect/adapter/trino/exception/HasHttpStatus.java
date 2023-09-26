package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

/**
 * Common interface that allows exceptions to specify an HTTP status associated with the error they represent.
 */
public interface HasHttpStatus {

    /**
     * Returns the HTTP status code that best categorizes this exception from the point of view of a caller
     * into this microservice.
     */
    HttpStatus httpStatus();
}
