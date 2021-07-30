package com.dnastack.ga4gh.search.adapter.presto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrestoError {

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private Integer errorCode;

    @JsonProperty("errorName")
    private String errorName;

    @JsonProperty("errorType")
    private String errorType;
}
