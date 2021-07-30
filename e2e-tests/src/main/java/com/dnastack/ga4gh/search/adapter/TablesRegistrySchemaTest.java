package com.dnastack.ga4gh.search.adapter;

import com.dnastack.ga4gh.search.adapter.test.model.DataModel;
import com.dnastack.ga4gh.search.adapter.test.model.Table;
import com.dnastack.ga4gh.search.adapter.test.model.TableInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

public class TablesRegistrySchemaTest extends BaseE2eTest {

    private static String SEARCH_PRESTO_PUBLIC_APP_NAME = "search-presto-public";
    private static String SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE = "This test is run for 'search-presto-public' ONLY";

    private static String egaTable1 = "dbgap_demo.scr_ega.scr_egapancreatic_sample_multi";
    private static String geccoTable1 = "dbgap_demo.scr_gecco_susceptibility.sample_attributes_multi";
    private static String geccoTable2 = "dbgap_demo.scr_gecco_susceptibility.sample_multi";
    private static String geccoTable3 = "dbgap_demo.scr_gecco_susceptibility.subject_multi";
    private static String geccoTable4 = "dbgap_demo.scr_gecco_susceptibility.subject_phenotypes_multi";

    @Test
    public void getTableInfo_should_returnDataModelFromTablesRegistryForEgaTable1() throws Exception {
        // Run this test for 'search-presto-public' only
        assumeTrue(SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE,
                SEARCH_PRESTO_PUBLIC_APP_NAME.equals(optionalEnv("APP_NAME", null)));

        DataModel expectedDataModel = getExpectedDataModelFromTestResources("egaDataModel1.json");

        fetchAndVerifyTableInfo(egaTable1, expectedDataModel);
        fetchAndVerifyTableData(egaTable1, expectedDataModel);
    }

    @Test
    public void getTableInfo_should_returnDataModelFromTablesRegistryForGeccoTable1() throws Exception {
        // Run this test for 'search-presto-public' only
        assumeTrue(SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE,
                SEARCH_PRESTO_PUBLIC_APP_NAME.equals(optionalEnv("APP_NAME", null)));

        DataModel expectedDataModel = getExpectedDataModelFromTestResources("geccoDataModel1.json");

        fetchAndVerifyTableInfo(geccoTable1, expectedDataModel);
        fetchAndVerifyTableData(geccoTable1, expectedDataModel);
    }

    @Test
    public void getTableInfo_should_returnDataModelFromTablesRegistryForGeccoTable2() throws Exception {
        // Run this test for 'search-presto-public' only
        assumeTrue(SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE,
                SEARCH_PRESTO_PUBLIC_APP_NAME.equals(optionalEnv("APP_NAME", null)));

        DataModel expectedDataModel = getExpectedDataModelFromTestResources("geccoDataModel2.json");

        fetchAndVerifyTableInfo(geccoTable2, expectedDataModel);
        fetchAndVerifyTableData(geccoTable2, expectedDataModel);
    }

    @Test
    public void getTableInfo_should_returnDataModelFromTablesRegistryForGeccoTable3() throws Exception {
        // Run this test for 'search-presto-public' only
        assumeTrue(SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE,
                SEARCH_PRESTO_PUBLIC_APP_NAME.equals(optionalEnv("APP_NAME", null)));;

        DataModel expectedDataModel = getExpectedDataModelFromTestResources("geccoDataModel3.json");

        fetchAndVerifyTableInfo(geccoTable3, expectedDataModel);
        fetchAndVerifyTableData(geccoTable3, expectedDataModel);
    }

    @Test
    public void getTableInfo_should_returnDataModelFromTablesRegistryForGeccoTable4() throws Exception {
        // Run this test for 'search-presto-public' only
        assumeTrue(SEARCH_PRESTO_PUBLIC_TEST_VALIDATION_MESSAGE,
                SEARCH_PRESTO_PUBLIC_APP_NAME.equals(optionalEnv("APP_NAME", null)));

        DataModel expectedDataModel = getExpectedDataModelFromTestResources("geccoDataModel4.json");

        fetchAndVerifyTableInfo(geccoTable4, expectedDataModel);
        fetchAndVerifyTableData(geccoTable4, expectedDataModel);
    }

    private DataModel getExpectedDataModelFromTestResources(String dataModelJsonFile) throws IOException {
        DataModel expectedDataModel;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(dataModelJsonFile)) {
            ObjectMapper objectMapper = new ObjectMapper();
            expectedDataModel = objectMapper.readValue(is, DataModel.class);
        }
        return expectedDataModel;
    }

    private void fetchAndVerifyTableInfo(String tableName, DataModel expectedDataModel) throws IOException {
        TableInfo tableInfo = SearchE2eTest.searchApiGetRequest("/table/" + tableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo, not(nullValue()));
        Assertions.assertThat(tableInfo.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    private void fetchAndVerifyTableData(String tableName, DataModel expectedDataModel) throws IOException {
        Table tableData = SearchE2eTest.searchApiGetRequest("/table/" + tableName + "/data", 200, Table.class);
        assertThat(tableData, not(nullValue()));
        tableData = SearchE2eTest.searchApiGetAllPages(tableData);
        Assertions.assertThat(tableData.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }
}
