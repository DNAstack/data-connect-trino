package com.dnastack.ga4gh.dataconnect.adapter.trino;

@FunctionalInterface
public interface TrinoDataTransformer {

    /**
     *
     * @param content The data to transform
     * @return A transformed version of content.  Content may need to be transformed if it is in a format incompatible
     * with the data connect specification.
     */
    Object transform(String content);
}
