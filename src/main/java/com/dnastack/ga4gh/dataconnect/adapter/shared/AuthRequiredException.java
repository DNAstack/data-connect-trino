package com.dnastack.ga4gh.dataconnect.adapter.shared;

import lombok.Getter;

@Getter
public class AuthRequiredException extends RuntimeException {
    private final DataConnectAuthRequest authorizationRequest;

    public AuthRequiredException(DataConnectAuthRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
    }
}
