package com.dnastack.ga4gh.search.adapter.shared;

import com.dnastack.ga4gh.search.adapter.presto.PrestoError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;

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

    public UserFacingError(PrestoError prestoError, String traceId) {
        this.message = prestoError.getMessage();
        this.errorCode = prestoError.getErrorCode();
        this.errorName = prestoError.getErrorName();
        this.errorType = prestoError.getErrorType();
        this.traceId = traceId;
    }

    public UserFacingError(String message, String traceId) {
        this(message, null, null, null, traceId);
    }
}
