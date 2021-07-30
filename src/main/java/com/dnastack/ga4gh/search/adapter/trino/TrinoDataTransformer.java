package com.dnastack.ga4gh.search.adapter.trino;

@FunctionalInterface
public interface TrinoDataTransformer {

    /**
     *
     * @param content The data to transform
     * @return A transformed version of content.  Content may need to be transformed if it is in a format incompatible
     * with the search specification.
     */
    Object transform(String content);
}
