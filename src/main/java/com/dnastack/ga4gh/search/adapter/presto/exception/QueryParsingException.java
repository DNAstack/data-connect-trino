package com.dnastack.ga4gh.search.adapter.presto.exception;

public class QueryParsingException extends RuntimeException {

    public QueryParsingException() {
        super();
    }

    public QueryParsingException(String message) {
        super(message);
    }

    public QueryParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryParsingException(Throwable cause) {
        super(cause);
    }
}
