package com.dnastack.ga4gh.search.adapter.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("ext_expires_in")
    private Long extExpiresIn;

    @JsonProperty("expires_on")
    private Long expiresOn;

    @JsonProperty("not_before")
    private Long notBefore;

    @JsonProperty("resource")
    String resource;

    @JsonProperty("scope")
    String scope;

}
