package com.dnastack.ga4gh.search.adapter.presto.exception;

public class DataModelNotDefinedException extends RuntimeException {
    public DataModelNotDefinedException(String message) {
        super(message);
    }
}
