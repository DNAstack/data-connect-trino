package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserFacingError {
    @JsonProperty("message")
    private String message;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("error_name")
    private String errorName;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("trace_id")
    private String traceId;

}
