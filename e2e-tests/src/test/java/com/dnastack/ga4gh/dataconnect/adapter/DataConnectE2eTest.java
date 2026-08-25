package com.dnastack.ga4gh.dataconnect.adapter;

import com.dnastack.ga4gh.dataconnect.adapter.test.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.Method.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
class DataConnectE2eTest extends BaseE2eTest {
    // Expected values for the 'format' field of the JSON schema returned accompanying various fields of the
    // date-time test table.
    private static final Map<String, String> EXPECTED_FORMATS = Map.of(
        "thedate", "date",
        "thetime", "time",
        "thetimestamp", "date-time",
        "thetimestampwithtimezone", "date-time",
        "thetimestampwithouttimezone", "date-time",
        "thetimewithouttimezone", "time",
        "thetimewithtimezone", "time");
    //"thetimewithtimezone", "time");   //Blocked by https://github.com/prestosql/presto/issues/4715

    //These expected values assume the remote server is UTC.
    private static final Map<String, Map<String, String>> EXPECTED_VALUES = Map.of(
            "LosAngeles", Map.of(
                    "thedate", "2020-05-27",
                    "thetime", "12:22:27.000",
                    "thetimestamp", "2020-05-27T12:22:27.000",
                    "thetimestampwithtimezone", "2020-05-27T12:22:27.000-08:00",
                    "thetimestampwithouttimezone", "2020-05-27T12:22:27.000",
                    "thetimewithouttimezone", "12:22:27.000",
                    "thetimewithtimezone", "12:22:27.000-08:00"),
            "UTC", Map.of(
                    "thedate", "2020-05-27",
                    "thetime", "12:22:27.000",
                    "thetimestamp", "2020-05-27T12:22:27.000",
                    "thetimestampwithtimezone", "2020-05-27T12:22:27.000Z",
                    "thetimestampwithouttimezone", "2020-05-27T12:22:27.000",
                    "thetimewithouttimezone", "12:22:27.000",
                    "thetimewithtimezone", "12:22:27.000Z"));


    private static final String TEST_DATE = "2020-05-27";
    private static final String TEST_TIME_LOS_ANGELES = "12:22:27.000-08:00";
    private static final String TEST_TIME_UTC = "12:22:27.000+00:00";
    private static final String TEST_DATE_TIME_LOS_ANGELES = "2020-05-27 12:22:27.000-08:00";
    private static final String TEST_DATE_TIME_UTC = "2020-05-27 12:22:27.000+00:00";

    private static final int MAX_REAUTH_ATTEMPTS = 10;

    /**
     * [Optional] Name of a catalog that's expected to contain ar least one schema. eg: E2E_SHOW_SCHEMA_FOR_CATALOG_NAME="publisher"
     */
    private static final @Nullable String SHOW_SCHEMA_FOR_CATALOG_NAME = optionalEnv("E2E_SHOW_SCHEMA_FOR_CATALOG_NAME");

    /**
     * [Optional] Name of a catalog.schema that's expected to contain at least one table. eg: E2E_SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME="publisher.public"
     */
    private static final @Nullable String SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME = optionalEnv("E2E_SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME");

    private static final String PUBLISHER_DATA_RESOURCE_URI = optionalEnv("E2E_PUBLISHER_DATA_RESOURCE_URI", "http://localhost:8095/");
    private static final String INDEXING_SERVICE_URI = optionalEnv("E2E_INS_BASE_URI", "http://localhost:8094");
    private static final String INDEXING_SERVICE_RESOURCE_URI = optionalEnv("E2E_INS_RESOURCE_URI", "http://localhost:8094/");

    private static final boolean GLOBAL_METHOD_SECURITY_ENABLED = Boolean.parseBoolean(optionalEnv("E2E_GLOBAL_METHOD_SECURITY_ENABLED", "false"));
    private static final boolean SCOPE_CHECKING_ENABLED = Boolean.parseBoolean(optionalEnv("E2E_SCOPE_CHECKING_ENABLED", "false"));


    /**
     * Lazily initialized if Google credentials are needed by the test. Always access via {@link #getGoogleCredentials()}.
     */
    private static @Nullable GoogleCredentials googleCredentials;

    /**
     * These are the extra credentials of the type that the Data Connect API challenges for. They will be added to the
     * RestAssured requests created by {@link #givenAuthenticatedRequest(String...)}.
     */
    private static final Map<String, String> extraCredentials = new HashMap<>();

    /** What the suite asks for on data-connect-trino itself, to call its API. */
    private static final List<String> dataConnectScopes = List.of("data-connect:query", "data-connect:data", "data-connect:info");

    /**
     * What the suite asks for on publisher-data, the resource Trino's access control asks collection-service about.
     * Creating and dropping this run's catalog needs authority over the data, which is why this list has
     * {@code data-connect:manage} and {@link #dataConnectScopes} does not.
     */
    private static final List<String> publisherDataScopes =
            List.of("data-connect:query", "data-connect:data", "data-connect:info", "data-connect:manage");

    /**
     * When this test run started. It names the run's catalog, so that a later run can tell how long ago a leftover
     * catalog was created and clean it up.
     */
    private static final long TEST_RUN_ID = System.currentTimeMillis();

    /** Borne by every catalog this suite creates, in every environment, so that strays are recognisable. */
    private static final String TEST_CATALOG_PREFIX = "dnastack_e2etest_data_connect_trino_";

    /**
     * A catalog of this run's own, created in {@link #createTestCatalog()} and dropped in
     * {@link #dropTestCatalog()}.
     * <p>
     * It is the run's own rather than one the environment provides because a catalog some connector is pointed at
     * gets indexed, and an indexed table competes with the library entry
     * {@link #getTableInfo_should_returnTheLibrarySchema_when_theLibraryDescribesTheTable()} registers for the same table — two
     * entries under one preferred name, which the library reports as an error. Nobody registers a connection for a
     * catalog created seconds ago under a name no configuration mentions.
     */
    private static final String TEST_CATALOG = TEST_CATALOG_PREFIX + TEST_RUN_ID;

    private static final String TEST_SCHEMA = "e2e";

    /**
     * How old a leftover catalog has to be before a later run treats it as abandoned rather than as belonging to a
     * run still in progress. Runs take minutes, so hours is a wide margin.
     */
    private static final Duration ABANDONED_CATALOG_AGE = Duration.ofHours(2);

    private static final Pattern TEST_CATALOG_PATTERN = Pattern.compile(Pattern.quote(TEST_CATALOG_PREFIX) + "(\\d+)");

    /**
     * How many pages of the table listing to follow at each end of the next page trail. An environment can have
     * hundreds of schemas, and hence hundreds of pages, whose middle exercises nothing the ends do not.
     */
    private static final int PAGES_TO_FOLLOW_AT_EACH_END = 3;

    /**
     * The column list of a table whose rows are wide enough that a few hundred of them span several response pages.
     */
    private static final String PAGINATION_TABLE_COLUMNS = "id integer, bogusfield varchar";

    private static @Nullable TestTables testTables;

    @BeforeAll
    static void createTestTables() {
        dropAbandonedTestCatalogs();
        createTestCatalog();

        log.info("Setting up test tables");
        testTables = TestTables.create();
        log.info("Done setting up test tables");
    }

    /**
     * Drops the run's catalog, and with it every schema and table the run created. Trino drops a catalog's contents
     * along with it, so the tests do not have to keep track of what they made.
     */
    @AfterAll
    static void dropTestCatalog() {
        log.info("Dropping catalog {}", TEST_CATALOG);
        try {
            dataConnectQuery("DROP CATALOG " + TEST_CATALOG);
        } catch (Exception e) {
            log.error("Failed to drop catalog {}. A later run will clean it up once it is {} old.",
                    TEST_CATALOG, ABANDONED_CATALOG_AGE, e);
        }
    }

    private static void createTestCatalog() {
        log.info("Creating catalog {}", TEST_CATALOG);
        try {
            dataConnectQuery(
                    "CREATE CATALOG %s USING memory WITH (\"memory.max-data-per-node\" = '10MB')".formatted(TEST_CATALOG));
            dataConnectQuery("CREATE SCHEMA %s.%s".formatted(TEST_CATALOG, TEST_SCHEMA));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create catalog " + TEST_CATALOG, e);
        }
    }

    /**
     * Drops the catalogs of earlier runs that ended before they could drop their own. A run that is killed leaves its
     * catalog resident in Trino, where in a long-lived environment it would accumulate.
     */
    private static void dropAbandonedTestCatalogs() {
        final long abandonedBefore = TEST_RUN_ID - ABANDONED_CATALOG_AGE.toMillis();

        List<String> abandoned;
        try {
            abandoned = dataConnectQueryNoErrorCheck("SHOW CATALOGS").getData().stream()
                    .map(row -> String.valueOf(row.get("Catalog")))
                    .filter(catalog -> isAbandonedTestCatalog(catalog, abandonedBefore))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not list catalogs, so any abandoned ones are left in place.", e);
            return;
        }

        for (String catalog : abandoned) {
            log.info("Dropping abandoned test catalog {}", catalog);
            try {
                dataConnectQuery("DROP CATALOG " + catalog);
            } catch (Exception e) {
                log.warn("Failed to drop abandoned test catalog {}. Continuing.", catalog, e);
            }
        }
    }

    /** Reports whether the given catalog was created by this suite, before the given moment. */
    private static boolean isAbandonedTestCatalog(String catalog, long createdBefore) {
        Matcher matcher = TEST_CATALOG_PATTERN.matcher(catalog);
        if (!matcher.matches()) {
            return false;
        }
        try {
            return Long.parseLong(matcher.group(1)) < createdBefore;
        } catch (NumberFormatException e) {
            log.warn("Catalog {} is named like one of ours but does not end in a timestamp. Leaving it alone.", catalog);
            return false;
        }
    }

    @BeforeEach
    final void beforeEachTest() {
        extraCredentials.clear();
    }

    /**
     * Returns the tables created for this test run.
     *
     * @throws IllegalStateException if the tables have not been created yet.
     */
    private static TestTables tables() {
        if (testTables == null) {
            throw new IllegalStateException("Test tables have not been created. Did createTestTables() run?");
        }
        return testTables;
    }

    /**
     * A table in this run's own catalog.
     * <p>
     * The only way to get one is {@link #create}, and every table so created is dropped along with the catalog in
     * {@link #dropTestCatalog()}. Cleanup therefore needs no bookkeeping: a table that exists at all is in the
     * catalog that goes away.
     */
    static final class TestTable {

        private final String unqualifiedName;
        private final String qualifiedName;

        private TestTable(String unqualifiedName) {
            this.unqualifiedName = unqualifiedName;
            this.qualifiedName = TEST_CATALOG + "." + TEST_SCHEMA + "." + unqualifiedName;
        }

        /**
         * Creates a table in this run's catalog.
         *
         * @param name says what the table is for. It is lowercased to match the way Trino folds unquoted
         *             identifiers, and needs nothing to make it unique because the catalog is unique already.
         * @param columnDefinitions the column list of the CREATE TABLE statement, for example
         *                          {@code "id integer, bogusfield varchar"}.
         * @throws IllegalStateException if the table cannot be created.
         */
        static TestTable create(String name, String columnDefinitions) {
            TestTable table = new TestTable(name.toLowerCase(Locale.ROOT));
            table.execute("CREATE TABLE %s (%s)", columnDefinitions);
            return table;
        }

        /** This table's name qualified by catalog and schema, which is how Trino and Data Connect refer to it. */
        String qualifiedName() {
            return qualifiedName;
        }

        /** This table's name on its own, with no catalog or schema qualifier. */
        String unqualifiedName() {
            return unqualifiedName;
        }

        /**
         * Runs one statement against this table through the Data Connect API.
         *
         * @param sqlFormat a {@link String#formatted} template. Its first {@code %s} is filled in with this table's
         *                  qualified name, and any remaining format specifiers are filled from {@code args}.
         * @throws IllegalStateException if the statement fails.
         */
        void execute(String sqlFormat, Object... args) {
            Object[] formatArgs = new Object[args.length + 1];
            formatArgs[0] = qualifiedName;
            System.arraycopy(args, 0, formatArgs, 1, args.length);

            String sql = sqlFormat.formatted(formatArgs);
            try {
                dataConnectQuery(sql);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute: " + sql, e);
            }
        }
    }

    /**
     * The tables this test run creates in the in-memory test catalog, populated with the rows the tests expect.
     */
    private record TestTables(TestTable json, TestTable dateTime, TestTable pagination) {

        static TestTables create() {
            log.info("Creating table for JSON support tests.");
            TestTable json = TestTable.create("json_test", "id varchar(25), data json");
            insertJsonRow(json, "string", "\"Hello\"");
            insertJsonRow(json, "boolean", "true");
            insertJsonRow(json, "number", "1.0");
            insertJsonRow(json, "json_object", "{\"name\": \"Foo\", \"age\": 25}");
            insertJsonRow(json, null, null);
            insertJsonRow(json, "array_of_various_types", "[\"Hello\", true, 1.0, {\"name\": \"Foo\"}, null, [1,2]]");
            insertJsonRow(json, "array_of_json_objects", "[{\"name\": \"Foo\", \"age\": 25}, {\"name\": \"Boo\", \"age\": 52}]");

            log.info("Creating table for date/time support tests.");
            TestTable dateTime = TestTable.create("date_time_test", """
                    zone VARCHAR(255),
                    thedate DATE,
                    thetime TIME,
                    thetimestamp TIMESTAMP,
                    thetimestampwithtimezone TIMESTAMP WITH TIME ZONE,
                    thetimestampwithouttimezone TIMESTAMP WITHOUT TIME ZONE,
                    thetimewithouttimezone TIME WITHOUT TIME ZONE,
                    thetimewithtimezone TIME WITH TIME ZONE""");
            insertDateTimeRow(dateTime, "LosAngeles", TEST_TIME_LOS_ANGELES, TEST_DATE_TIME_LOS_ANGELES);
            insertDateTimeRow(dateTime, "UTC", TEST_TIME_UTC, TEST_DATE_TIME_UTC);

            log.info("Creating table for pagination tests.");
            TestTable pagination = TestTable.create("pagination", PAGINATION_TABLE_COLUMNS);
            insertPaginationRows(pagination, 120);

            awaitRowsVisible(json);
            return new TestTables(json, dateTime, pagination);
        }

        /**
         * Queries the given table until it returns rows: we have seen tests run before their setup data was
         * visible, and suspect the Trino memory connector of being eventually consistent.
         *
         * @throws IllegalStateException if the table still has no rows after several attempts.
         */
        private static void awaitRowsVisible(TestTable table) {
            Table lastResult = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    lastResult = dataConnectQueryNoErrorCheck("SELECT * FROM " + table.qualifiedName());
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to query " + table.qualifiedName(), e);
                }
                if (lastResult.getErrors().isEmpty() && !lastResult.getData().isEmpty()) {
                    return;
                }
            }
            throw new IllegalStateException("Tables didn't settle: " + lastResult);
        }
    }

    /**
     * Inserts one row into a table shaped like the JSON test table.
     *
     * @param id the row's id, or null to insert a row with no id.
     * @param jsonLiteral the JSON document to store, or null to insert a row with no document.
     */
    private static void insertJsonRow(TestTable table, @Nullable String id, @Nullable String jsonLiteral) {
        table.execute(
                "INSERT INTO %s (id, data) VALUES(%s, json_parse(%s))",
                quoteSqlString(id),
                quoteSqlString(jsonLiteral));
    }

    /**
     * Inserts one row into a table shaped like the date/time test table, recording the same moment in every one of
     * its date, time and timestamp columns.
     *
     * @param zone names the row, and says which zone the given time and date/time are expressed in.
     * @param time a time literal including a zone offset, for example {@code 12:22:27.000-08:00}.
     * @param dateTime a date and time literal including a zone offset, for example
     *                 {@code 2020-05-27 12:22:27.000-08:00}.
     */
    private static void insertDateTimeRow(TestTable table, String zone, String time, String dateTime) {
        table.execute(
                "INSERT INTO %s(zone, thedate, thetime, thetimestamp, thetimestampwithtimezone, thetimestampwithouttimezone, thetimewithouttimezone, thetimewithtimezone)"
                    + " VALUES(%s, date %s, time %s, timestamp %s, timestamp %s, timestamp %s, time %s, time %s)",
                quoteSqlString(zone),
                quoteSqlString(TEST_DATE),
                quoteSqlString(time),
                quoteSqlString(dateTime),
                quoteSqlString(dateTime),
                quoteSqlString(dateTime),
                quoteSqlString(time),
                quoteSqlString(time));
    }

    /**
     * Fills the given table's {@code bogusfield} column with the requested number of rows, each wide enough that
     * about 600 of them fill one Trino page of response data.
     */
    private static void insertPaginationRows(TestTable table, int rows) {
        List<String> values = new ArrayList<>(rows);
        for (int i = 0; i < rows; ++i) {
            values.add("(REPLACE('testValue_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx_%s', 'x', 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'))".formatted(i));
        }
        table.execute("INSERT INTO %s (bogusfield) VALUES %s", String.join(", ", values));
    }

    /**
     * Renders the given value as a SQL string literal, or as {@code null} if it is null.
     */
    private static String quoteSqlString(@Nullable String s) {
        if (s == null) {
            return "null";
        }
        return "'" + s.replace("'", "''") + "'";
    }


    private ListTableResponse getFirstPageOfTableListing() throws Exception {
        ListTableResponse listTableResponse = getTableListingPage("/tables");

        assertThat(listTableResponse.getIndex()).isNotNull();

        for (int i = 0; i < listTableResponse.getIndex().size(); ++i) {
            assertThat(listTableResponse.getIndex().get(i).getUrl()).as("URL of index entry %d".formatted(i)).isNotNull();
            assertThat(listTableResponse.getIndex().get(i).getPage()).as("page number of index entry %d".formatted(i)).isEqualTo(i);
        }
        return listTableResponse;
    }

    private ListTableResponse getTableListingPage(String url) throws Exception {
        return GLOBAL_METHOD_SECURITY_ENABLED
                ? getListTableResponse(url)
                : dataConnectApiGetRequest(url, 200, ListTableResponse.class);
    }

    private ListTableResponse getListTableResponse(String url) {
        String bearerToken = getToken(dataConnectScopes, List.of(dataConnectAdapterResource));
        String searchAuthorizationToken = getToken(publisherDataScopes, List.of(PUBLISHER_DATA_RESOURCE_URI));

        Map<String, Object> headers = new HashMap<>();
        headers.put("GA4GH-Search-Authorization", String.format("userToken=%s", searchAuthorizationToken));

        return given()
                .auth().oauth2(bearerToken)
                .headers(headers)
                .get(url)
                .then().log().ifValidationFails()
                .statusCode(200)
                .extract()
                .as(ListTableResponse.class);
    }

    @EnabledIfEnvironmentVariable(named = "E2E_INDEXING_SERVICE_ENABLED", matches = "true", disabledReason = "This test requires data-connect-trino to be hooked up to indexing-service")
    @Test
    void getTableInfo_should_returnTheLibrarySchema_when_theLibraryDescribesTheTable() throws IOException {
        final String indexingServiceBearerToken = getToken(List.of("ins:library:write"), List.of(INDEXING_SERVICE_RESOURCE_URI + "library/") );

        final String paginationTableName = tables().pagination().qualifiedName();

        log.info("Verifying table info for [{}]", paginationTableName);
        TableInfo tableInfo = dataConnectApiGetRequest("/table/" + paginationTableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo.getName()).isEqualTo(paginationTableName);
        assertThat(tableInfo.getDataModel()).isNotNull();
        assertThat(tableInfo.getDataModel().getId()).isNotNull();
        assertThat(tableInfo.getDataModel().getSchema()).isNotNull();
        assertThat(tableInfo.getDataModel().getProperties()).isNotNull();
        assertThat(tableInfo.getDataModel().getProperties().entrySet()).isNotEmpty();

        log.info("Adding the table to the library table with a custom JSON schema, and scheduling its deletion");
        final String libraryItemId = given()
            .auth().oauth2(indexingServiceBearerToken)
            .contentType(ContentType.JSON)
            .body(
                LibraryItem.builder()
                    .type("table")
                    .dataSourceName("nonexistent_connection")
                    .dataSourceType("search:e2e:nonexistent-connection")
                    .name(paginationTableName)
                    .sourceKey(paginationTableName)
                    .description("Generated by DataConnectE2eTest")
                    .preferredName(paginationTableName)
                    .aliases(List.of())
                    .preferredColumnNames(Map.of())
                    .jsonSchema(objectMapper.writeValueAsString(Map.of("$comment", "This is the custom schema from library")))
                    .size(123L)
                    .sizeUnit("row")
                    .dataSourceUrl("https://search-e2e-test.dnastack.com/")
                    .build()
            )
            .post(URI.create(INDEXING_SERVICE_URI).resolve("/library"))
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("name", equalTo(paginationTableName))
            .body("preferredName", equalTo(paginationTableName))
            .extract()
            .jsonPath()
            .getString("id");
        afterThisTest(() ->
            given()
                .auth().oauth2(indexingServiceBearerToken)
                .delete(URI.create(INDEXING_SERVICE_URI).resolve("/library/" + libraryItemId))
                .then()
                .statusCode(204)
        );

        log.info("Verifying that the custom schema is fetched for [{}]", paginationTableName);
        tableInfo = dataConnectApiGetRequest("/table/" + paginationTableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo.getDataModel().getId()).isNull();
        assertThat(tableInfo.getDataModel().getProperties()).isNull();
        assertThat(tableInfo.getDataModel().getAdditionalProperties())
                .containsEntry("$comment", "This is the custom schema from library");
    }

    public static Collection<Object[]> getTestParams() {
        final Pattern groupPattern = Pattern.compile("^E2E_([A-Za-z\\d]+)_EXPECTED_DATA_MODEL$");
        List<String> groups = System.getenv().keySet().stream().map(key -> {
            Matcher matcher = groupPattern.matcher(key);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }).filter(Objects::nonNull).toList();

        return groups.stream().flatMap(group -> {
            String tableName = requiredEnv(String.format("E2E_%s_TABLE_NAME", group));
            String expectedJsonDataModel = requiredEnv(String.format("E2E_%s_EXPECTED_DATA_MODEL", group));
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{ tableName, expectedJsonDataModel });
            return params.stream();
        }).collect(Collectors.toList());
    }

    static boolean noExpectedDataModelsAreConfigured() {
        return getTestParams().isEmpty();
    }

    @ParameterizedTest(name = "Testing table with name [{0}]")
    @MethodSource("getTestParams")
    @DisabledIf(value = "noExpectedDataModelsAreConfigured", disabledReason = "No test data found when looking for environment variables of pattern E2E_%s_EXPECTED_DATA_MODEL")
    void getTableInfoAndData_should_returnExpectedDataModel(String tableName, String expectedJsonDataModel) throws Exception {
        DataModel expectedDataModel = objectMapper.readValue(expectedJsonDataModel, DataModel.class);
        fetchAndVerifyTableInfo(tableName, expectedDataModel);
        fetchAndVerifyTableData(tableName, expectedDataModel);
    }

    private void fetchAndVerifyTableInfo(String tableName, DataModel expectedDataModel) throws IOException {
        TableInfo tableInfo = dataConnectApiGetRequest("/table/" + tableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo).isNotNull();
        assertThat(tableInfo.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    private void fetchAndVerifyTableData(String tableName, DataModel expectedDataModel) throws IOException {
        Table tableData = dataConnectApiGetRequest("/table/" + tableName + "/data", 200, Table.class);
        assertThat(tableData).isNotNull();
        dataConnectApiGetAllPages(tableData);
        assertThat(tableData.getDataModel()).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    @Test
    void getTableInfo_should_describeAJsonColumnAsAnObject() throws IOException {
        String jsonTableName = tables().json().qualifiedName();
        Table tableInfo = dataConnectApiGetRequest(String.format("/table/%s/info", jsonTableName), 200, Table.class);
        assertThat(tableInfo).isNotNull();
        assertThat(tableInfo.getName()).isEqualTo(jsonTableName);
        assertThat(tableInfo.getDataModel().getProperties().get("data").getType()).isEqualTo("object");
    }

    @Test
    void getTableData_should_returnAJsonColumnAsAnObject() throws IOException {
        Table tableData = dataConnectApiGetRequest("/table/" + tables().json().qualifiedName() + "/data", 200, Table.class);
        assertThat(tableData).isNotNull();
        dataConnectApiGetAllPages(tableData);

        for (Map<String, Object> data : tableData.getData()) {
            checkJsonData(String.valueOf(data.get("id")), data.get("data"));
        }
    }

    @Test
    void getTableInfo_should_describeDateAndTimeColumnsAsFormattedStrings() throws IOException {
        String qualifiedTableName = tables().dateTime().qualifiedName();
        TableInfo tableInfo = dataConnectApiGetRequest("/table/" + qualifiedTableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo).isNotNull();
        assertThat(tableInfo.getName()).isEqualTo(qualifiedTableName);
        assertThat(tableInfo.getDataModel()).isNotNull();
        assertThat(tableInfo.getDataModel().getId()).isNotNull();
        assertThat(tableInfo.getDataModel().getSchema()).isNotNull();
        assertThat(tableInfo.getDataModel().getProperties()).isNotNull();
        assertThat(tableInfo.getDataModel().getProperties().entrySet()).isNotEmpty();

        EXPECTED_FORMATS.forEach((key, value) -> {
            assertThat(tableInfo.getDataModel().getProperties()).containsKey(key);
            assertThat(tableInfo.getDataModel().getProperties().get(key).getFormat()).isEqualTo(value);
            assertThat(tableInfo.getDataModel().getProperties().get(key).getType()).isEqualTo("string");
        });
    }

    @Test
    void searchQuery_should_returnAnInlineColumnSchema_when_ga4ghTypeIsGivenOne() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        ColumnSchema columnSchema = ColumnSchema.builder()
            .format("foo")
            .type("string")
            .build();

        String json = objectMapper.writeValueAsString(columnSchema);
        String q = "SELECT ga4gh_type(bogusfield, '%s') FROM %s".formatted(json, tables().pagination().qualifiedName());
        DataConnectRequest query = new DataConnectRequest(q);
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties().keySet()).containsExactly("bogusfield");
        assertThat(result.getDataModel().getProperties().get("bogusfield").getFormat()).isEqualTo("foo");
        assertThat(result.getDataModel().getProperties().get("bogusfield").getType()).isEqualTo("string");
    }

    @Test
    void searchQuery_should_returnTheRefUnderTheColumnName_when_ga4ghTypeHasNoAlias() throws IOException {
        DataConnectRequest query = new DataConnectRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') FROM %s",
            tables().pagination().qualifiedName()));
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties().keySet()).containsExactly("bogusfield");
        assertThat(result.getDataModel().getProperties().get("bogusfield").getRef()).isEqualTo("http://path/to/whatever.com");
    }

    @Test
    void searchQuery_should_returnTheRefUnderTheAlias_when_ga4ghTypeIsAliasedWithAs() throws IOException {
        DataConnectRequest query = new DataConnectRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') as bf FROM %s",
            tables().pagination().qualifiedName()));
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties().keySet()).containsExactly("bf");
        assertThat(result.getDataModel().getProperties().get("bf").getRef()).isEqualTo("http://path/to/whatever.com");
    }

    @Test
    void searchQuery_should_returnTheRefUnderTheAlias_when_ga4ghTypeIsAliasedWithoutAs() throws IOException {
        DataConnectRequest query = new DataConnectRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') bf FROM %s",
            tables().pagination().qualifiedName()));
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties().keySet()).containsExactly("bf");
        assertThat(result.getDataModel().getProperties().get("bf").getRef()).isEqualTo("http://path/to/whatever.com");
    }

    @Test
    void searchQuery_should_returnTheRefUnderTheAlias_when_ga4ghTypeIsGivenAJsonRef() throws IOException {
        DataConnectRequest query = new DataConnectRequest(String.format("SELECT ga4gh_type(bogusfield, '{\"$ref\":\"http://path/to/whatever.com\"}') as bf FROM %s",
            tables().pagination().qualifiedName()));
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties().keySet()).containsExactly("bf");
        assertThat(result.getDataModel().getProperties().get("bf").getRef()).isEqualTo("http://path/to/whatever.com");
    }

    private void assertDatesAndTimesHaveCorrectValuesForZone(String zone, Map<String, String> expectedValues) throws IOException {
        DataConnectRequest query = new DataConnectRequest(
                "SELECT * FROM %s WHERE zone='%s'".formatted(tables().dateTime().qualifiedName(), zone));
        log.info("Reading back the {} row of the date/time table: {}", zone, query);

        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);

        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        } else if (result.getData().size() > 1) {
            throw new RuntimeException("Found more than one test table entry for " + zone + " time zone, but only one was expected.");
        }

        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();

        final Map<String, ColumnSchema> properties = result.getDataModel().getProperties();
        final Map<String, Object> row = result.getData().getFirst();
        EXPECTED_FORMATS.forEach((columnName, expectedColumnFormat) -> {
            assertThat(properties.get(columnName).getFormat())
                    .as("format of column %s in the %s row".formatted(columnName, zone))
                    .isEqualTo(expectedColumnFormat);
            assertThat(properties.get(columnName).getType())
                    .as("type of column %s in the %s row".formatted(columnName, zone))
                    .isEqualTo("string");
            assertThat(row.get(columnName))
                    .as("value of column %s in the %s row".formatted(columnName, zone))
                    .isEqualTo(expectedValues.get(columnName));
        });
    }

    @Test
    void searchQuery_should_returnDateAndTimeValuesInTheZoneTheyWereInsertedIn() throws IOException {
        for (Map.Entry<String, Map<String, String>> e : EXPECTED_VALUES.entrySet()) {
            log.info("Checking date and time was inserted correctly for zone {}", e.getKey());
            assertDatesAndTimesHaveCorrectValuesForZone(e.getKey(), e.getValue());
        }
    }

    @Test
    void getTables_should_returnAPageIndexMatchingTheNextPageTrail() throws Exception {
        ListTableResponse firstPage = getFirstPageOfTableListing();
        List<PageIndexEntry> index = firstPage.getIndex();

        if (index.size() == 1) {
            assertThat(firstPage.getPagination()).as("pagination of a listing that fits on one page").isNull();
            return;
        }

        int pagesAtTheHead = Math.min(PAGES_TO_FOLLOW_AT_EACH_END, index.size());
        followNextPageTrail(firstPage, 0, pagesAtTheHead, index);

        int firstPageOfTheTail = Math.max(pagesAtTheHead, index.size() - PAGES_TO_FOLLOW_AT_EACH_END);
        if (firstPageOfTheTail < index.size()) {
            log.info("Skipping to page {} of {} to follow the end of the trail", firstPageOfTheTail, index.size());
            followNextPageTrail(
                    getTableListingPage(index.get(firstPageOfTheTail).getUrl().toString()),
                    firstPageOfTheTail,
                    index.size() - firstPageOfTheTail,
                    index);
        }
    }

    /**
     * Follows the next page trail from the given page, requiring each page to link to the URL its successor has in
     * the index, and the last page of the listing to end the trail rather than link onward.
     *
     * @param page the page to start from, which must be the page numbered {@code firstPageNumber} in the index.
     * @param firstPageNumber the index position of {@code page}.
     * @param pagesToFollow how many pages to examine before stopping, counting {@code page} itself.
     * @param index the page index the first page of the listing reported.
     */
    private void followNextPageTrail(
            ListTableResponse page, int firstPageNumber, int pagesToFollow, List<PageIndexEntry> index)
            throws Exception {

        for (int i = 0; i < pagesToFollow; i++) {
            final int pageNumber = firstPageNumber + i;
            log.info("Page {}: following the trail", pageNumber);

            if (page.getErrors() != null) {
                log.warn("Page {} contained errors, continuing anyway: {}", pageNumber, page.getErrors());
            }

            if (pageNumber == index.size() - 1) {
                assertThat(page.getPagination())
                        .as("pagination of page %d, the last page of the listing".formatted(pageNumber))
                        .isNull();
                return;
            }

            assertThat(page.getPagination()).as("pagination of page %d".formatted(pageNumber)).isNotNull();
            assertThat(page.getPagination().getNextPageUrl())
                    .as("next page URL of page %d".formatted(pageNumber))
                    .isEqualTo(index.get(pageNumber + 1).getUrl());

            if (i < pagesToFollow - 1) {
                page = getTableListingPage(page.getPagination().getNextPageUrl().toString());
            }
        }
    }

    @Test
    void deleteNextPageUrl_should_terminateQuery() throws IOException {
        TestTable table = TestTable.create("query_termination", PAGINATION_TABLE_COLUMNS);
        insertPaginationRows(table, 600);

        DataConnectRequest query = new DataConnectRequest("SELECT * FROM " + table.qualifiedName());
        log.info("Running query {} and following the next page URL", query);
        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        String nextPageUrl = result.getPagination().getNextPageUrl().toString();

        log.info("Sending a DELETE request to the next page URL, then asserting that the right error response is returned when retrying the GET request to the next page URL");
        sendDeleteRequest(nextPageUrl);
        result = dataConnectApiGetRequest(nextPageUrl, 400, Table.class);
        assertThat(result.getErrors()).as("errors from the cancelled query's next page").hasSize(1);
        assertThat(result.getErrors().getFirst().getDetails().toLowerCase()).as("error detail from the cancelled query's next page").contains("canceled"); // Trino uses the american spelling
    }

    private Table executeSearchQueryOnVariedTypes() throws Exception {
        String query = "SELECT ("+
                       "((42428060 IS NULL) OR MOD(42428060, 1337) = 0) "+
                       "AND 'A' = 'A' "+
                       "AND 'T' = 'T' "+
                       ")  as \"exists\", "+
                       "'bogusValue' as varcharField, "+
                       "1245359 as integerField, "+
                       "array[1,2,3] as simpleArray, "+
                       "array[array[1,2,3], array[4,5,6]] as multiDimArray, "+
                       "MAP(ARRAY['myFirstRow', 'mySecondRow'], ARRAY[cast(row('row1FieldValue1', 'row1FieldValue2') as row(firstField varchar, secondField varchar)), cast(row('row2FieldValue1', 'row2FieldValue2') as row(firstField varchar, secondField varchar))]) as mapField, "+
                       "CAST(MAP(ARRAY['jsonkey1', 'jsonkey2', 'jsonkey3'], ARRAY['foo', 'bar', 'baz']) AS JSON) as jsonField, "+
                       "ARRAY[ "+
                       "  cast(row('ExampleDataset', true, array[row('Sample', 'Info')]) as row(datasetId varchar, \"exists\" boolean, \"info\" row(\"key\" varchar, \"value\" varchar) array)) "+
                       "] as datasetAlleleResponses";

        DataConnectRequest dataConnectRequest = new DataConnectRequest(query);
        log.info("Querying one row of every type Data Connect supports: {}", query);
        Table result = dataConnectApiRequest(Method.POST, "/search", dataConnectRequest, 200, Table.class);
        dataConnectApiGetAllPages(result);

        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query + ", but none were found.");
        } else if (result.getDataModel() == null) {
            throw new RuntimeException("No data model was returned for query "+query);
        }
        return result;
    }

    @Test
    void searchQuery_should_returnTheExpectedRows_when_theResultHasAColumnOfEveryType() throws Exception {
        Table result = executeSearchQueryOnVariedTypes();
        List<Map<String, Object>> expectedData;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("variedTypesData.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            expectedData = objectMapper.readValue(is, new TypeReference<>(){});
        }
        List<Map<String, Object>> actualData = result.getData();
        assertThat(actualData).usingRecursiveComparison().isEqualTo(expectedData);
    }

    @Test
    void searchQuery_should_returnTheExpectedDataModel_when_theResultHasAColumnOfEveryType() throws Exception {

        Table result = executeSearchQueryOnVariedTypes();
        DataModel expectedDataModel;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("variedTypesDataModel.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            expectedDataModel = objectMapper.readValue(is, DataModel.class);
        }

        DataModel actualDataModel = result.getDataModel();
        assertThat(actualDataModel).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    @Test
    void searchQuery_should_return400WithMessageAndTraceId_when_theSqlIsMalformed() throws Exception {
        DataConnectRequest query = new DataConnectRequest("SELECT * FROM FROM E2ETEST LIMIT STRAWBERRY");
        Table data = dataConnectUntilBadRequest(query);
        runBasicAssertionOnTableErrorList(data.getErrors());
        assertThat(data.getErrors().getFirst().getStatus()).isEqualTo(400);
    }

    @Test
    void searchQuery_should_return400WithMessageAndTraceId_when_aSelectedColumnDoesNotExist() throws Exception {
        DataConnectRequest query = new DataConnectRequest(
                "SELECT e2etest_olywolypolywoly FROM " + tables().pagination().qualifiedName() + " LIMIT 10");
        Table data = dataConnectUntilBadRequest(query);
        runBasicAssertionOnTableErrorList(data.getErrors());
        assertThat(data.getErrors().getFirst().getStatus()).isEqualTo(400);
    }

    @Test
    void searchQuery_should_returnRowsAndADataModel() throws Exception {

        DataConnectRequest query = new DataConnectRequest(
                "SELECT * FROM " + tables().pagination().qualifiedName() + " LIMIT 10");
        log.info("Querying the pagination table for a handful of rows: {}", query);


        Table result = dataConnectApiRequest(Method.POST, "/search", query, 200, Table.class);
        while (result.getPagination() != null) {
            result = dataConnectApiGetRequest(result.getPagination().getNextPageUrl().toString(), 200, Table.class);
            if (result.getDataModel() != null) {
                break;
            }
        }

        assertThat(result).isNotNull();
        assertThat(result.getDataModel()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotNull();
        assertThat(result.getDataModel().getProperties()).isNotEmpty();
    }

    @Test
    void getTableInfo_should_return404WithMessageAndTraceId_when_theCatalogDoesNotExist() throws Exception {
        final String trinoTableWithBadCatalog = "e2etest_olywlypolywoly.public." + tables().pagination().unqualifiedName();
        TableInfo info = dataConnectApiGetRequest("/table/" + trinoTableWithBadCatalog + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().getFirst().getStatus()).isEqualTo(404);
    }

    @Test
    void getTableInfo_should_return404WithMessageAndTraceId_when_theSchemaDoesNotExist() throws Exception {
        final String trinoTableWithBadSchema =
                TEST_CATALOG + ".e2etest_olywolypolywoly." + tables().pagination().unqualifiedName();
        TableInfo info = dataConnectApiGetRequest("/table/" + trinoTableWithBadSchema + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().getFirst().getStatus()).isEqualTo(404);
    }

    @Test
    void getTableInfo_should_return404WithMessageAndTraceId_when_theTableDoesNotExist() throws Exception {
        final String trinoTableWithBadTable = TEST_CATALOG + "." + TEST_SCHEMA + "." + "e2etest_olywolypolywoly";
        TableInfo info = dataConnectApiGetRequest("/table/" + trinoTableWithBadTable + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().getFirst().getStatus()).isEqualTo(404);
    }

    @Test
    void getTableInfo_should_return404WithMessageAndTraceId_when_theTableNameIsNotQualified() throws Exception {
        final String trinoTableWithBadTable = "e2etest_olywolypolywoly";
        TableInfo info = dataConnectApiGetRequest("/table/" + trinoTableWithBadTable + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().getFirst().getStatus()).isEqualTo(404);
    }

    @Test
    void getTableData_should_returnDataAndDataModel() throws Exception {
        Table tableData = dataConnectApiGetRequest("/table/" + tables().pagination().qualifiedName() + "/data", 200, Table.class);
        assertThat(tableData).isNotNull();
        dataConnectApiGetAllPages(tableData);
        assertThat(tableData.getData()).isNotNull();
        assertThat(tableData.getData()).isNotEmpty();
        assertThat(tableData.getDataModel()).isNotNull();
        assertThat(tableData.getDataModel().getSchema()).isNotNull();
        assertThat(tableData.getDataModel().getProperties()).isNotNull();
        assertThat(tableData.getDataModel().getProperties().entrySet()).isNotEmpty();
    }

    @Test
    void getTables_should_return403_when_theTokenLacksTheInfoScope() {
        assumeThat(GLOBAL_METHOD_SECURITY_ENABLED).as("E2E_GLOBAL_METHOD_SECURITY_ENABLED").isTrue();
        assumeThat(SCOPE_CHECKING_ENABLED).as("E2E_SCOPE_CHECKING_ENABLED").isTrue();

        givenAuthenticatedRequest("junk_scope")
            .when()
            .get("/tables")
            .then()
            .log().ifValidationFails()
            .statusCode(403)
            .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    @Test
    void getTableData_should_return403_when_theTokenLacksTheDataScope() {
        assumeThat(GLOBAL_METHOD_SECURITY_ENABLED).as("E2E_GLOBAL_METHOD_SECURITY_ENABLED").isTrue();
        assumeThat(SCOPE_CHECKING_ENABLED).as("E2E_SCOPE_CHECKING_ENABLED").isTrue();

        givenAuthenticatedRequest("junk_scope")
            .when()
            .get("/table/{tableName}/data", tables().pagination().qualifiedName())
            .then()
            .log().ifValidationFails()
            .statusCode(403)
            .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    @Test
    void searchQuery_should_return403_when_theTokenLacksTheDataAndQueryScopes() {
        assumeThat(GLOBAL_METHOD_SECURITY_ENABLED).as("E2E_GLOBAL_METHOD_SECURITY_ENABLED").isTrue();
        assumeThat(SCOPE_CHECKING_ENABLED).as("E2E_SCOPE_CHECKING_ENABLED").isTrue();

        DataConnectRequest testDataConnectRequest = new DataConnectRequest(
                "SELECT * FROM %s LIMIT 10".formatted(tables().json().qualifiedName()));

        givenAuthenticatedRequest("data-connect:data") // but not data-connect:query
                .when()
                .contentType(ContentType.JSON)
                .body(testDataConnectRequest)
                .post("/search")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));

        givenAuthenticatedRequest("data-connect:query") // but not data-connect:data
                .when()
                .contentType(ContentType.JSON)
                .body(testDataConnectRequest)
                .post("/search")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    @Test
    void searchShowSchemas_should_returnAtLeastOneSchema() throws IOException {
        assumeThat(SHOW_SCHEMA_FOR_CATALOG_NAME)
                .as("E2E_SHOW_SCHEMA_FOR_CATALOG_NAME")
                .isNotNull();

        DataConnectRequest query = new DataConnectRequest("SHOW SCHEMAS FROM " + SHOW_SCHEMA_FOR_CATALOG_NAME);
        assertQueryReturnsRows(query, "Schema");
    }

    /**
     * Executes the query and requires that it includes at least one result row, and has a column with the given name.
     *
     * @param query the SQL query to post to the /search endpoint
     * @param expectedColumnName the name of a column that must appear in the result set
     * @throws IOException if an HTTP call to the server errors out
     */
    private static void assertQueryReturnsRows(DataConnectRequest query, String expectedColumnName) throws IOException {
        log.info("Querying for rows with a {} column: {}", expectedColumnName, query);

        Table result = dataConnectApiRequest(POST, "/search", query, 200, Table.class);
        dataConnectApiGetAllPages(result);

        assertThat(result.getData()).as("rows returned by %s".formatted(query.getQuery())).isNotNull();

        assertThat(result.getDataModel()).as("data model returned by %s".formatted(query.getQuery())).isNotNull();
        assertThat(result.getDataModel().getProperties()).as("data model properties returned by %s".formatted(query.getQuery())).isNotNull();

        assertThat(result.getDataModel().getProperties()).as("data model properties returned by %s".formatted(query.getQuery())).containsKey(expectedColumnName);
        assertThat(result.getData()).as("rows returned by %s".formatted(query.getQuery())).isNotEmpty();
    }

    @Test
    void searchShowTables_should_returnAtLeastOneTable() throws IOException {
        assumeThat(SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME)
                .as("E2E_SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME")
                .isNotNull();
        DataConnectRequest query = new DataConnectRequest("SHOW TABLES FROM " + SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME);
        assertQueryReturnsRows(query, "Table");
    }

    static void runBasicAssertionOnTableErrorList(List<TableError> errors) {
        assertThat(errors).isNotNull();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().getTitle()).isNotNull();
        assertThat(errors.getFirst().getDetails()).isNotNull();
    }

    /**
     * Sends the given query and retrieves all of its pages.
     *
     * @param sql the query to run.
     * @throws AssertionError if any page of the response reported an error.
     */
    static void dataConnectQuery(String sql) throws IOException {
        Table result = dataConnectQueryNoErrorCheck(sql);
        assertThat(result.getErrors()).as("errors from %s".formatted(sql)).isEmpty();
    }

    static Table dataConnectQueryNoErrorCheck(String sql) throws IOException {
        Table table = dataConnectApiRequest(POST, "/search", Map.of("query", sql), 200, Table.class);
        dataConnectApiGetAllPages(table);
        return table;
    }

    /**
     * Retrieves all rows of the given table by following pagination links page by page, and appends to the given table object.
     *
     * @param table the table with the initial row set and pagination link.
     */
    static void dataConnectApiGetAllPages(Table table) throws IOException {
        while (table.getPagination() != null && table.getPagination().getNextPageUrl() != null) {
            String nextPageUri = table.getPagination().getNextPageUrl().toString();
            Table nextResult = dataConnectApiGetRequest(nextPageUri, 200, Table.class);
            if (nextResult.getData() != null) {
                log.info("Got {} results", nextResult.getData().size());
            }
            table.append(nextResult);
        }
    }

    /**
     * Performs a GET request with the currently configured authentication settings (both bearer tokens and extra
     * credentials requested by the Search API within the current test method). GA4GH Search API credential challenges
     * are handled automatically, and each challenge is validated.
     *
     * @param path           path and query parameters relative to E2E_BASE_URI, or any fully-qualified URL (useful for pagination
     *                       links)
     * @param expectedStatus the HTTP status the server must respond with
     * @param responseType   the Java type to map the response body into (using Jackson)
     * @return the server response body mapped to the given type
     * @throws IOException    if the HTTP request or JSON body parsing/mapping fails
     * @throws AssertionError if the HTTP response code does not match {@code expectedStatus} (except in the case of
     *                        well-formed Data Connect API credentials challenges from the server, which are automatically retried).
     */
    static <T> T dataConnectApiGetRequest(String path, int expectedStatus, Class<T> responseType) throws IOException {
        return dataConnectApiRequest(GET, path, null, expectedStatus, responseType);
    }

    public void sendDeleteRequest(String path) throws IOException {
        sendHttpRequest(DELETE, path, null)
            .then()
            .log().ifValidationFails(LogDetail.ALL)
            .statusCode(204);
    }

    /**
     * Performs an HTTP request with the currently configured authentication settings (both bearer tokens and extra
     * credentials requested by the Data Connect API within the current test method). GA4GH Data Connect API credential challenges
     * are handled automatically, and each challenge is validated.
     *
     * @param method         the HTTP method to use with the request
     * @param path           path and query parameters relative to E2E_BASE_URI, or any fully-qualified URL (useful for pagination
     *                       links)
     * @param body           the body to send with the request. If non-null, a JSON Content-Type header will be sent and the
     *                       request body will be the Jackson serialization of the given object. If null, no Content-Type and no
     *                       body will be sent.
     * @param expectedStatus the HTTP status the server must respond with
     * @param responseType   the Java type to map the response body into (using Jackson)
     * @return the server response body mapped to the given type
     * @throws IOException    if the HTTP request or JSON body parsing/mapping fails for either the request or the response.
     * @throws AssertionError if the HTTP response code does not match {@code expectedStatus} (except in the case of
     *                        well-formed Search API credentials challenges from the server, which are automatically retried).
     */
    static <T> T dataConnectApiRequest(Method method, String path, @Nullable Object body, int expectedStatus, Class<T> responseType) throws IOException {
        if (expectedStatus == 401) {
            fail("This method handles auth challenges and retries on 401. You can't use it when you want a 401 response.");
        }

        return sendHttpRequest(method, path, body)
            .then()
            .log().ifValidationFails()
            .statusCode(expectedStatus)
            .extract()
            .as(responseType);
    }

    /**
     * Executes a data connect query and follows nextUri links until a response returns HTTP 400.
     * If that status is never reached, an assertion error is thrown.
     *
     * @return The last page of the response (does not contain the accumulated rows from previous pages).
     * @throws AssertionError if the expected error is not encountered by the last page, or when a different error is encountered.
     */
    private static Table dataConnectUntilBadRequest(Object query) throws IOException {
        final int expectedErrorStatus = HttpStatus.SC_BAD_REQUEST;

        Response response = sendHttpRequest(Method.POST, "/search", query);
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            Table table = response.then().log().ifValidationFails(LogDetail.ALL).extract().as(Table.class);
            while (table.getPagination() != null && table.getPagination().getNextPageUrl() != null) {
                String nextPageUri = table.getPagination().getNextPageUrl().toString();
                Response nextPageResponse = sendHttpRequest(Method.GET, nextPageUri, null);
                log.info("Looking for status {} by following nextPageUri trail, most recent request returned {}",
                        expectedErrorStatus, nextPageResponse.getStatusCode());
                if (nextPageResponse.getStatusCode() == expectedErrorStatus) {
                    return nextPageResponse.then().log().ifValidationFails(LogDetail.ALL).extract().as(Table.class);
                } else if (nextPageResponse.getStatusCode() != HttpStatus.SC_OK) {
                    throw new AssertionError("Unexpected response status " + response.getStatusCode() + " (sent GET /" + nextPageUri + ", expecting " + expectedErrorStatus + " or 200");
                } else {
                    table = nextPageResponse.then().log().ifValidationFails(LogDetail.ALL).extract().as(Table.class);
                }
            }
        } else if (response.getStatusCode() == expectedErrorStatus) {
            return response.then().log().ifValidationFails(LogDetail.ALL).extract().as(Table.class);
        }
        throw new AssertionError("Expected to receive status " + expectedErrorStatus + " somewhere on the nextUri trail, but never found it.");
    }

    private static Response sendHttpRequest(Method method, String path, @Nullable Object body) throws IOException {
        Optional<HttpAuthChallenge> wwwAuthenticate;
        for (int attempt = 0; attempt < MAX_REAUTH_ATTEMPTS; attempt++) {

            // this request includes all the extra credentials we have been challenged for so far
            String defaultScope = optionalEnv("E2E_WALLET_DEFAULT_SCOPE");
            RequestSpecification requestSpec = defaultScope == null ? givenAuthenticatedRequest()
                : givenAuthenticatedRequest(defaultScope);
            if (body != null) {
                requestSpec
                    .contentType(ContentType.JSON)
                    .body(body);
            }
            Response response = requestSpec.request(method, path);

            if (response.getStatusCode() == 401) {
                wwwAuthenticate = extractAuthChallengeHeader(response);
                log.info("Got auth challenge header {}", wwwAuthenticate);
                if (wwwAuthenticate.isEmpty()) {
                    throw new AssertionError("Got HTTP 401 without WWW-Authenticate header");
                }

                if ("invalid_token".equals(wwwAuthenticate.get().getParams().get("error"))) {
                    log.info("Try running again with E2E_LOG_TOKENS=true to see what's wrong");
                }

                assertThat(wwwAuthenticate.get().getScheme()).as("auth challenge scheme, which is unexpected unless E2E_GLOBAL_METHOD_SECURITY_ENABLED is true").isEqualTo("GA4GH-Search");

                DataConnectAuthChallengeBody challengeBody = response.as(DataConnectAuthChallengeBody.class);
                DataConnectAuthRequest dataConnectAuthRequest = challengeBody.getAuthorizationRequest();

                assertAuthChallengeIsValid(wwwAuthenticate.get(), dataConnectAuthRequest);
                String token = supplyCredential(dataConnectAuthRequest);

                String existingCredential = extraCredentials.put(dataConnectAuthRequest.getKey(), token);

                assertThat(existingCredential)
                    .as("credential already supplied for %s, so a second challenge for it means the token was rejected"
                            .formatted(dataConnectAuthRequest))
                    .isNull();

                //noinspection UnnecessaryContinue
                continue;
            } else {
                return response;
            }
        }
        throw new AssertionError(
            "Exceeded MAX_REAUTH_ATTEMPTS (" + MAX_REAUTH_ATTEMPTS + ")." +
                " Tokens gathered so far: " + extraCredentials.keySet());
    }

    private static void assertAuthChallengeIsValid(HttpAuthChallenge wwwAuthenticate, DataConnectAuthRequest dataConnectAuthRequest) {
        assertThat(dataConnectAuthRequest).as("authorization-request in the auth challenge body").isNotNull();
        assertThat(dataConnectAuthRequest.getKey()).isNotNull();
        assertThat(wwwAuthenticate.getParams().get("realm")).as("realm in the WWW-Authenticate header").isEqualTo(dataConnectAuthRequest.getKey());
        assertThat(dataConnectAuthRequest.getResourceDescription()).isNotNull();
    }

    private static String supplyCredential(DataConnectAuthRequest dataConnectAuthRequest) throws IOException {
        log.info("Handling auth challenge {}", dataConnectAuthRequest);

        // first check for a configured token
        // a real client wouldn't use the key to decide what to get; that would complect the client with catalog naming choices!
        // a real client should do a credential lookup using the type and resource-description!
        String tokenEnvName = "E2E_SEARCH_CREDENTIALS_" + dataConnectAuthRequest.getKey().toUpperCase();
        String configuredToken = optionalEnv(tokenEnvName);
        if (configuredToken != null) {
            log.info("Using {} to satisfy auth challenge", tokenEnvName);
            return configuredToken;
        }

        if (dataConnectAuthRequest.getResourceType().equals("bigquery")) {
            log.info("Using Google Application Default credentials to satisfy auth challenge");
            return getGoogleCredentials().getAccessToken().getTokenValue();
        }

        throw new RuntimeException("Can't satisfy auth challenge " + dataConnectAuthRequest + ": unknown resource type. Try defining " + tokenEnvName + ".");
    }

    private static GoogleCredentials getGoogleCredentials() throws IOException {
        if (googleCredentials == null) {
            googleCredentials = GoogleCredentials.getApplicationDefault();
            googleCredentials.refresh();
        }
        return googleCredentials;
    }

    private static Optional<HttpAuthChallenge> extractAuthChallengeHeader(Response response) {
        String authChallengeString = response.header("WWW-Authenticate");
        if (authChallengeString != null) {
            try {
                return Optional.of(HttpAuthChallenge.fromString(authChallengeString));
            } catch (final Exception e) {
                throw new AssertionError("Failed to parse WWW-Authenticate header [" + authChallengeString + "]", e);
            }
        }
        return Optional.empty();
    }

    static RequestSpecification givenAuthenticatedRequest(String... scopes) {
        RequestSpecification req = given();

        // Add auth if auth properties are configured
        if (GLOBAL_METHOD_SECURITY_ENABLED) {
            String accessToken = getToken(List.of(scopes), List.of(dataConnectAdapterResource));
            req.auth().oauth2(accessToken);
            if (optionalEnv("E2E_LOG_TOKENS", "false").equalsIgnoreCase("true")) {
                log.info("Using access token {}", accessToken);
            }
        }

        String searchAuthorizationToken = getToken(publisherDataScopes, List.of(PUBLISHER_DATA_RESOURCE_URI));
        req.header("GA4GH-Search-Authorization", String.format("userToken=%s", searchAuthorizationToken));

        // add extra credentials
        extraCredentials.forEach((k, v) -> req.header("GA4GH-Search-Authorization", k + "=" + v));

        return req;
    }

    /**
     * Asserts that the JSON value stored under the given id came back as the type and value it was inserted as.
     *
     * @param id the value of the row's {@code id} column, which says what was inserted into its {@code data} column.
     * @param data the row's {@code data} column, as the Data Connect API returned it.
     */
    private static void checkJsonData(String id, Object data) {
        JsonNode node = objectMapper.valueToTree(data);
        switch (id) {
            case "number" -> {
                assertThat(node.getNodeType()).isEqualTo(JsonNodeType.NUMBER);
                assertThat(node.numberValue()).isEqualTo(1.0);
            }
            case "string" -> {
                assertThat(node.getNodeType()).isEqualTo(JsonNodeType.STRING);
                assertThat(node.textValue()).isEqualTo("Hello");
            }
            case "boolean" -> {
                assertThat(node.getNodeType()).isEqualTo(JsonNodeType.BOOLEAN);
                assertThat(node.booleanValue()).isTrue();
            }
            case "null" -> assertThat(node.getNodeType()).isEqualTo(JsonNodeType.NULL);
            case "json_object" -> {
                assertThat(node.getNodeType()).isEqualTo(JsonNodeType.OBJECT);
                assertThat(node.get("age").numberValue()).isEqualTo(25);
                assertThat(node.get("name").textValue()).isEqualTo("Foo");
            }
            case "array_of_various_types" -> assertThat(node)
                    .extracting(JsonNode::getNodeType)
                    .containsExactly(JsonNodeType.STRING, JsonNodeType.BOOLEAN, JsonNodeType.NUMBER,
                            JsonNodeType.OBJECT, JsonNodeType.NULL, JsonNodeType.ARRAY);
            case "array_of_json_objects" -> assertThat(node)
                    .extracting(JsonNode::getNodeType)
                    .containsExactly(JsonNodeType.OBJECT, JsonNodeType.OBJECT);
            default -> { }
        }
    }

}
