package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

public class DataModelNotDefinedException extends RuntimeException {
    public DataModelNotDefinedException(String message) {
        super(message);
    }
}
