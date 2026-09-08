package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

/**
 * A caller's extra credentials, sent in the {@code GA4GH-Search-Authorization} header, were refused at the
 * request boundary. The message of every subclass is answered to the caller, so it says what is wrong with the
 * header without repeating any credential value back.
 */
public abstract class ClientSuppliedCredentialsException extends RuntimeException implements HasHttpStatus {

    protected ClientSuppliedCredentialsException(String message) {
        super(message);
    }

    protected ClientSuppliedCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
