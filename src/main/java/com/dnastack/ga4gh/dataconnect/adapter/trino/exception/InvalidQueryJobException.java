package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidQueryJobException extends RuntimeException implements HasHttpStatus {

    private String queryJobId;

    public InvalidQueryJobException(String queryJobId) {
        super("Query job not found");
        this.queryJobId = queryJobId;
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
