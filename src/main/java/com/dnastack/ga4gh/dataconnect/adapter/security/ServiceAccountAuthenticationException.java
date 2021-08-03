package com.dnastack.ga4gh.dataconnect.adapter.security;

public class ServiceAccountAuthenticationException extends RuntimeException {

    public ServiceAccountAuthenticationException(String message) {
        super(message);
    }

    public ServiceAccountAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
