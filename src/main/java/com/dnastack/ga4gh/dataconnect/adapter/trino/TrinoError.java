package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrinoError {

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private Integer errorCode;

    @JsonProperty("errorName")
    private String errorName;

    @JsonProperty("errorType")
    private String errorType;

    private FailureInfo failureInfo = new FailureInfo(); // ensure this is never null

    @Data
    public static class FailureInfo {
        private String type;
        private String message;
    }
}
