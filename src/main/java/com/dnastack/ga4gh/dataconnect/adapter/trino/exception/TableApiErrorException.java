package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.model.TableError;
import lombok.Getter;
import lombok.NonNull;

import java.util.function.Function;

public class TableApiErrorException extends RuntimeException {

    @Getter
    private final Function<TableError, Object> responseBodyGenerator;

    public TableApiErrorException(@NonNull Throwable cause, @NonNull Function<TableError, Object> responseBodyGenerator) {
        super(cause);
        this.responseBodyGenerator = responseBodyGenerator;
    }
}
