package com.dnastack.ga4gh.search.adapter.presto;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrestoSearchAdapterTest {

    @Test
    public void biFunctionPatternTest() {
        String jsonFunctionQuery = "select id, phenopacket from sample_phenopackets.ga4gh_tables.gecco_phenopackets " +
                "where json_extract_scalar(pp.phenopacket, '$.subject.sex') = 'MALE' limit 3";
        assertFalse(PrestoSearchAdapter.biFunctionPattern.matcher(jsonFunctionQuery).find());

        String ga4ghTypeFunctionQuery = "SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') FROM tableX";
        assertTrue(PrestoSearchAdapter.biFunctionPattern.matcher(ga4ghTypeFunctionQuery).find());
    }

}