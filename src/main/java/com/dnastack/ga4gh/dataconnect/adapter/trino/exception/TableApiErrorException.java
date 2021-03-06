package com.dnastack.ga4gh.dataconnect.adapter.trino.exception;

import com.dnastack.ga4gh.dataconnect.model.TableError;
import lombok.Getter;
import lombok.NonNull;

import java.util.function.Function;

public class TableApiErrorException extends RuntimeException {
    @Getter
    private final Exception previousException;

    @Getter
    private final Function<TableError, Object> errorSupplier;

    public TableApiErrorException(@NonNull Exception previousException, @NonNull Function<TableError, Object> errorSupplier) {
        this.previousException = previousException;
        this.errorSupplier = errorSupplier;
    }
}
