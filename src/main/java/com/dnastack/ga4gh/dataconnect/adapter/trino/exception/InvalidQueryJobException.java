package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import lombok.Getter;

@Getter
public class InvalidQueryJobException extends RuntimeException {

    private String queryJobId;

    public InvalidQueryJobException(String queryJobId) {
        super();
        this.queryJobId = queryJobId;
    }

    public InvalidQueryJobException(String queryJobId, String message) {
        super(message);
        this.queryJobId = queryJobId;
    }

    public InvalidQueryJobException(String queryJobId, String message, Throwable cause) {
        super(message, cause);
        this.queryJobId = queryJobId;
    }

    public InvalidQueryJobException(String queryJobId, Throwable cause) {
        super(cause);
        this.queryJobId = queryJobId;
    }
}
