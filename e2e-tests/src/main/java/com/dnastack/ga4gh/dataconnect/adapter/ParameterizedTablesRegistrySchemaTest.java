package com.dnastack.ga4gh.dataconnect.adapter;

import com.dnastack.ga4gh.dataconnect.adapter.test.model.DataModel;
import com.dnastack.ga4gh.dataconnect.adapter.test.model.Table;
import com.dnastack.ga4gh.dataconnect.adapter.test.model.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
@EnabledIfEnvironmentVariable(named = "E2E_INDEXING_SERVICE_ENABLED", matches = "true", disabledReason = "This app doesn't have indexing-service properties configured.")
public class ParameterizedTablesRegistrySchemaTest extends BaseE2eTest {

    public static Collection<Object[]> getTestParams() {
        final Pattern groupPattern = Pattern.compile("^E2E_TRS_([A-Za-z\\d]+)_TABLE_NAME$");
        List<String> groups = System.getenv().keySet().stream().map(key -> {
            Matcher matcher = groupPattern.matcher(key);
            if (matcher.find())
                return matcher.group(1);
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return groups.stream().flatMap(group -> {
            String tableName = requiredEnv(String.format("E2E_TRS_%s_TABLE_NAME", group));
            String expectedJsonDataModel = requiredEnv(String.format("E2E_TRS_%s_EXPECTED_JSON_DATA_MODEL", group));
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{ tableName, expectedJsonDataModel });
            return params.stream();
        }).collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    public void getTableInfo_should_returnDataModelFromTablesRegistry(String tableName, String expectedJsonDataModel) throws Exception {
        DataModel expectedDataModel = objectMapper.readValue(expectedJsonDataModel, DataModel.class);
        fetchAndVerifyTableInfo(tableName, expectedDataModel);
        fetchAndVerifyTableData(tableName, expectedDataModel);
    }

    private void fetchAndVerifyTableInfo(String tableName, DataModel expectedDataModel) throws IOException {
        TableInfo tableInfo = DataConnectE2eTest.dataConnectApiGetRequest("/table/" + tableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo, not(nullValue()));
        Assertions.assertThat(tableInfo.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    private void fetchAndVerifyTableData(String tableName, DataModel expectedDataModel) throws IOException {
        Table tableData = DataConnectE2eTest.dataConnectApiGetRequest("/table/" + tableName + "/data", 200, Table.class);
        assertThat(tableData, not(nullValue()));
        DataConnectE2eTest.dataConnectApiGetAllPages(tableData);
        Assertions.assertThat(tableData.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }
}
