package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import org.springframework.http.HttpStatus;

/**
 * A caller's relayed {@code userToken} was issued for a different tenant than the request carrying it
 * addresses. Forbidden rather than bad request: the credential is well-formed and genuine, and it is the
 * authority it carries that the request is not entitled to.
 */
public class RelayedTokenTenantMismatchException extends ClientSuppliedCredentialsException {

    public RelayedTokenTenantMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
