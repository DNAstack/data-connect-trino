package com.dnastack.ga4gh.search.adapter.presto.exception;

import java.io.IOException;
import java.io.UncheckedIOException;

public class UncheckedTableDataConstructionException extends UncheckedIOException {
    public UncheckedTableDataConstructionException(IOException cause) {
        super(cause);
    }
}
