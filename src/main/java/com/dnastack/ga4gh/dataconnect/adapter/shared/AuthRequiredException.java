package com.dnastack.ga4gh.dataconnect.adapter.shared;

import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.HasHttpStatus;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthRequiredException extends RuntimeException implements HasHttpStatus {
    private final DataConnectAuthRequest authorizationRequest;

    public AuthRequiredException(DataConnectAuthRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
