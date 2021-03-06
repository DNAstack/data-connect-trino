package com.dnastack.ga4gh.dataconnect.adapter.shared;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Deprecated
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserFacingError {
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

    public UserFacingError(TrinoError trinoError, String traceId) {
        this.message = trinoError.getMessage();
        this.errorCode = trinoError.getErrorCode();
        this.errorName = trinoError.getErrorName();
        this.errorType = trinoError.getErrorType();
        this.traceId = traceId;
    }

    public UserFacingError(String message, String traceId) {
        this(message, null, null, null, traceId);
    }
}
