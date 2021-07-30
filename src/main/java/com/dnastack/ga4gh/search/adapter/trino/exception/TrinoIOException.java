package com.dnastack.ga4gh.search.adapter.trino.exception;

import java.io.IOException;
import java.io.UncheckedIOException;

public class TrinoIOException extends UncheckedIOException {
    public TrinoIOException(String message, IOException cause) {
        super(message, cause);
    }

    public TrinoIOException(IOException cause) {
        super(cause);
    }
}
