package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import java.io.IOException;
import java.io.UncheckedIOException;

public class UncheckedTableDataConstructionException extends UncheckedIOException {
    public UncheckedTableDataConstructionException(IOException cause) {
        super(cause);
    }
}
