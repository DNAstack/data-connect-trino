package com.dnastack.ga4gh.search.adapter.presto.exception;

import java.io.IOException;
import java.io.UncheckedIOException;

public class PrestoIOException extends UncheckedIOException {
    public PrestoIOException(String message, IOException cause) {
        super(message, cause);
    }

    public PrestoIOException(IOException cause) {
        super(cause);
    }
}
