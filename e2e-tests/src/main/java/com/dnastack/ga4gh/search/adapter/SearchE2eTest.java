package com.dnastack.ga4gh.search.adapter;

import com.dnastack.ga4gh.search.adapter.test.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.Method.GET;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

@Slf4j
public class SearchE2eTest extends BaseE2eTest {

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
    private static final Map<String, Map<String, String>> EXPECTED_VALUES = Map.of("LosAngeles",
                                                                                   Map.of("thedate",
                                                                                          "2020-05-27",
                                                                                          "thetime",
                                                                                          "12:22:27.000",
                                                                                          "thetimestamp",
                                                                                          "2020-05-27T12:22:27.000",
                                                                                          "thetimestampwithtimezone",
                                                                                          "2020-05-27T12:22:27.000-08:00",
                                                                                          "thetimestampwithouttimezone",
                                                                                          "2020-05-27T12:22:27.000",
                                                                                          "thetimewithouttimezone",
                                                                                          "12:22:27.000",
                                                                                          "thetimewithtimezone",
                                                                                          "12:22:27.000-08:00"),

                                                                                   "UTC",
                                                                                   Map.of("thedate",
                                                                                          "2020-05-27",
                                                                                          "thetime",
                                                                                          "12:22:27.000",
                                                                                          "thetimestamp",
                                                                                          "2020-05-27T12:22:27.000",
                                                                                          "thetimestampwithtimezone",
                                                                                          "2020-05-27T12:22:27.000Z",
                                                                                          "thetimestampwithouttimezone",
                                                                                          "2020-05-27T12:22:27.000",
                                                                                          "thetimewithouttimezone",
                                                                                          "12:22:27.000",
                                                                                          "thetimewithtimezone",
                                                                                          "12:22:27.000Z"));

    //"thetimewithtimezone", "12:22:27-07"); //Blocked by https://github.com/prestosql/presto/issues/4715


    private static final String INSERT_DATETIME_TEST_TABLE_ENTRY_TEMPLATE =
        "INSERT INTO %s(zone, thedate, thetime, thetimestamp, thetimestampwithtimezone, thetimestampwithouttimezone, thetimewithouttimezone, thetimewithtimezone)"
            + " VALUES('%s', date '%s', time '%s', timestamp '%s', timestamp '%s', timestamp '%s', time '%s', time '%s')";

    private static final String INSERT_PAGINATION_TEST_TABLE_ENTRY_TEMPLATE = "INSERT INTO %s(bogusfield) VALUES('%s')";

    private static final String INSERT_JSON_TEST_TABLE_ENTRY_TEMPLATE =
        "INSERT INTO %s (id, data) VALUES('foo', JSON '{\"name\": \"Foo\", \"age\": 25}')";

    private static final String TEST_DATE = "2020-05-27";
    private static final String TEST_TIME_LOS_ANGELES = "12:22:27.000-08:00";
    private static final String TEST_TIME_UTC = "12:22:27.000+00:00";
    private static final String TEST_DATE_TIME_LOS_ANGELES = "2020-05-27 12:22:27.000-08:00";
    private static final String TEST_DATE_TIME_UTC = "2020-05-27 12:22:27.000+00:00";

    private static final String CREATE_DATETIME_TEST_TABLE_TEMPLATE = "CREATE TABLE %s("
        + "zone varchar(255),"
        + "thedate DATE,"
        + "thetime time,"
        + "thetimestamp timestamp,"
        + "thetimestampwithtimezone timestamp with time zone,"
        + "thetimestampwithouttimezone timestamp without time zone,"
        + "thetimewithouttimezone time without time zone,"
        + "thetimewithtimezone time with time zone)";

    private static final String CREATE_PAGINATION_TEST_TABLE_TEMPLATE = "CREATE TABLE %s("
        + "id integer,"
        + "bogusfield varchar(64))";

    private static final String CREATE_JSON_TEST_TABLE_TEMPLATE = "CREATE TABLE %s (id varchar(8), data json)";

    private static final String DELETE_TEST_TABLE_TEMPLATE = "DROP TABLE %s";

    private static final int MAX_REAUTH_ATTEMPTS = 10;

    private static String prestoTestUri = optionalEnv("E2E_PRESTO_JDBCURI", "jdbc:presto://localhost:8091");;  // MUST be in format jdbc:presto://host:port
    private static String prestoTestUser = optionalEnv("E2E_PRESTO_USERNAME", null);; // optional
    private static String prestoTestPass = optionalEnv("E2E_PRESTO_PASSWORD", null);; // optional
    private static String prestoAudience = optionalEnv("E2E_PRESTO_AUDIENCE", null);; // optional
    private static String prestoScopes = optionalEnv("E2E_PRESTO_SCOPES", "full_access");;   // optional

    // test catalog name
    private static String inMemoryCatalog = optionalEnv("E2E_INMEMORY_TESTCATALOG", "memory"); //memory;

    // test schema name
    private static String inMemorySchema = optionalEnv("E2E_INMEMORY_TESTSCHEMA", "default"); //default;


    private static boolean globalMethodSecurityEnabled;
    private static boolean scopeCheckingEnabled;


    /**
     * Lazily initialized if Google credentials are needed by the test.
     */
    private static GoogleCredentials googleCredentials;

    /**
     * These are the extra credentials of the type that the Search API challenges for. They will be added to the
     * RestAssured requests created by {@link #givenAuthenticatedRequest(String...)}.
     */
    private static Map<String, String> extraCredentials = new HashMap<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        globalMethodSecurityEnabled = Boolean.parseBoolean(optionalEnv("E2E_GLOBAL_METHOD_SECURITY_ENABLED", "false"));
        scopeCheckingEnabled = Boolean.parseBoolean(optionalEnv("E2E_SCOPE_CHECKING_ENABLED", "false"));

        log.info("Setting up test tables");
        setupTestTables();
        log.info("Done setting up test tables");
    }

    static Connection getTestDatabaseConnection() throws SQLException {
        log.info("Logging in to {} with user {} and pass {}", prestoTestUri, prestoTestUser, prestoTestPass);
        log.info("Driver dump:");

        try {
            Class.forName("io.prestosql.jdbc.PrestoDriver");
            //Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ce) {
            throw new RuntimeException("Class not found", ce);
        }

        DriverManager.drivers().forEach(driver -> log.info("Got driver " + driver.toString()));

        Properties properties = new Properties();
        properties.setProperty("user", prestoTestUser != null ? prestoTestUser : "e2etestuser");
        properties.setProperty("SSL", prestoTestUri.contains("localhost") ? "false" : "true");

        if (prestoTestPass != null) {
            properties.setProperty("password", prestoTestPass);
        }

        if (prestoAudience != null) {
            properties.setProperty("accessToken", getToken(prestoAudience, prestoScopes));
        }

        Connection conn = DriverManager.getConnection(prestoTestUri, properties);
        return conn;
    }

    private static void assertTestDatabaseConnection() {
        try (Connection conn = getTestDatabaseConnection()) {
            if (conn != null) {
                log.info("Test database connection is valid for " + prestoTestUri);
            } else {
                throw new RuntimeException("Couldn't connect to test database with URI " + prestoTestUri);
            }
        } catch (SQLException e) {
            log.error("Error connecting to test database.  SQL State: {}", e.getSQLState(), e.getMessage());
            throw new RuntimeException("Couldn't connect to test database with URI " + prestoTestUri, e);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't connect to test database with URI " + prestoTestUri, e);
        }
    }

    private static String prestoDateTimeTestTable;
    private static String prestoPaginationTestTable;
    private static String prestoJsonTestTable;
    private static String unqualifiedPaginationTestTable; //just the table name (no catalog or schema)

    private static void setupTestTables() {
        String randomFactor = RandomStringUtils.randomAlphanumeric(16);
        List<String> queries = new LinkedList<>();

        // Create a test table for JSON support tests.
        prestoJsonTestTable = getFullyQualifiedTestTableName("jsonTest_" + randomFactor);
        queries.add(String.format(CREATE_JSON_TEST_TABLE_TEMPLATE, prestoJsonTestTable));
        queries.add(String.format(INSERT_JSON_TEST_TABLE_ENTRY_TEMPLATE, prestoJsonTestTable));

        // Create a test table for datetime tests.
        prestoDateTimeTestTable = getFullyQualifiedTestTableName("dateTimeTest_" + randomFactor);
        queries.add(String.format(CREATE_DATETIME_TEST_TABLE_TEMPLATE, prestoDateTimeTestTable));
        queries.add(String.format(
            INSERT_DATETIME_TEST_TABLE_ENTRY_TEMPLATE,
            prestoDateTimeTestTable,
            "LosAngeles",
            TEST_DATE, TEST_TIME_LOS_ANGELES,
            TEST_DATE_TIME_LOS_ANGELES,
            TEST_DATE_TIME_LOS_ANGELES,
            TEST_DATE_TIME_LOS_ANGELES,
            TEST_TIME_LOS_ANGELES,
            TEST_TIME_LOS_ANGELES
            // TEST_DATE_TIME_LOS_ANGELES //Blocked by https://github.com/prestosql/presto/issues/4715
        ));
        queries.add(String.format(
            INSERT_DATETIME_TEST_TABLE_ENTRY_TEMPLATE,
            prestoDateTimeTestTable,
            "UTC",
            TEST_DATE,
            TEST_TIME_UTC,
            TEST_DATE_TIME_UTC,
            TEST_DATE_TIME_UTC,
            TEST_DATE_TIME_UTC,
            TEST_TIME_UTC,
            TEST_TIME_UTC
            //TEST_DATE_TIME_UTC // Blocked by https://github.com/prestosql/presto/issues/4715
        ));

        // Create a test table with a bunch of bogus entries to test pagination.
        unqualifiedPaginationTestTable = "pagination_" + randomFactor;
        prestoPaginationTestTable = getFullyQualifiedTestTableName(unqualifiedPaginationTestTable);
        queries.add(String.format(CREATE_PAGINATION_TEST_TABLE_TEMPLATE, prestoPaginationTestTable));
        for (int i = 0; i < 120; ++i) {
            String testValue = "testValue_" + i;
            queries.add(String.format(INSERT_PAGINATION_TEST_TABLE_ENTRY_TEMPLATE, prestoPaginationTestTable, testValue));
        }

        try (Connection conn = getTestDatabaseConnection()) {
            Statement statement = conn.createStatement();

            queries.forEach(query -> {
                try {
                    statement.execute(query);
                } catch (SQLException se) {
                    log.error("Detected error while setting up the test tables.  SQL State: %s\n%s", se.getSQLState(), se.getMessage());
                    throw new RuntimeException("During test table setup, failed to execute: " + query, se);
                }
            });
        } catch (SQLException se) {
            log.error("Error connecting to the server.  SQL State: %s\n%s", se.getSQLState(), se.getMessage());
            throw new RuntimeException("Unable to setup test tables.", se);
        }
    }

    @AfterClass
    public static void removeTestTables() {
        if (prestoDateTimeTestTable != null) {
            log.info("Trying to remove datetime test table " + prestoDateTimeTestTable);
            try (Connection conn = getTestDatabaseConnection()) {
                Statement statement = conn.createStatement();

                statement.execute(String.format(DELETE_TEST_TABLE_TEMPLATE, prestoDateTimeTestTable));
                log.info("Successfully removed datetime test table " + prestoDateTimeTestTable);
                prestoDateTimeTestTable = null;

                statement.execute(String.format(DELETE_TEST_TABLE_TEMPLATE, prestoPaginationTestTable));
                log.info("Successfully removed pagination test table " + prestoPaginationTestTable);
                prestoPaginationTestTable = null;

                statement.execute(String.format(DELETE_TEST_TABLE_TEMPLATE, prestoJsonTestTable));
                log.info("Successfully removed json test table " + prestoJsonTestTable);
                prestoJsonTestTable = null;
            } catch (SQLException se) {
                log.error("Error setting up test tables.  SQL State: {}\n{}", se.getSQLState(), se.getMessage());
                throw new RuntimeException("Unable to setup test tables: ", se);
            }
        }
    }

    @Before
    public final void beforeEachTest() {
        extraCredentials.clear();
    }

    static String getFullyQualifiedTestTableName(String tableName) {
        return inMemoryCatalog + "." + inMemorySchema + "." + tableName;
    }


    private ListTableResponse getFirstPageOfTableListing() throws Exception {
        ListTableResponse listTableResponse = searchApiGetRequest("/tables", 200, ListTableResponse.class);
        assertThat(listTableResponse.getIndex(), not(nullValue()));

        for (int i = 0; i < listTableResponse.getIndex().size(); ++i) {
            assertThat(listTableResponse.getIndex().get(i).getUrl(), not(nullValue()));
            assertThat(listTableResponse.getIndex().get(i).getPage(), is(i));
        }
        return listTableResponse;
    }

    @Test
    public void jsonFieldIsDeclaredAsObject() throws IOException {
        String qualifiedTableName = prestoJsonTestTable;
        Table tableInfo = searchApiGetRequest(String.format("/table/%s/info", qualifiedTableName), 200, Table.class);
        assertThat(tableInfo, not(nullValue()));
        assertThat(tableInfo.getName(), equalTo(qualifiedTableName));

        assertThat(tableInfo.getDataModel().getProperties().get("data").getType(), equalTo("object"));
    }

    @Test
    public void jsonFieldIsRepresentedAsObject() throws IOException {
        Table tableData = searchApiGetRequest("/table/" + prestoJsonTestTable + "/data", 200, Table.class);
        assertThat(tableData, not(nullValue()));
        tableData = searchApiGetAllPages(tableData);

        Map<String, Object> jsonField = (Map<String, Object>) tableData.getData().get(0).get("data");

        assertThat(jsonField.get("name"), equalTo("Foo"));
        assertThat(jsonField.get("age"), equalTo(25));
    }

    @Test
    public void datesAndTimesHaveCorrectTypes() throws IOException {
        String qualifiedTableName = prestoDateTimeTestTable;
        TableInfo tableInfo = searchApiGetRequest("/table/" + qualifiedTableName + "/info", 200, TableInfo.class);
        assertThat(tableInfo, not(nullValue()));
        assertThat(tableInfo.getName(), equalTo(qualifiedTableName));
        assertThat(tableInfo.getDataModel(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getId(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getSchema(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getProperties(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getProperties().entrySet(), not(empty()));

        EXPECTED_FORMATS.entrySet().stream()
            .forEach((entry) -> {
                assertThat(tableInfo.getDataModel().getProperties(), hasKey(entry.getKey()));
                assertThat(tableInfo.getDataModel().getProperties().get(entry.getKey()).getFormat(), is(entry.getValue()));
                assertThat(tableInfo.getDataModel().getProperties().get(entry.getKey()).getType(), is("string"));
            });
    }

    @Test
    public void ga4ghTypeAsInlineGivesBackTypeAsInline() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        ColumnSchema columnSchema = ColumnSchema.builder()
            .format("foo")
            .type("string")
            .build();

        String json = objectMapper.writeValueAsString(columnSchema);
        String q = String.format("SELECT ga4gh_type(bogusfield, '" + json + "') FROM %s", prestoPaginationTestTable);
        SearchRequest query = new SearchRequest(q);
        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().keySet(), contains("bogusfield"));
        assertThat(result.getDataModel().getProperties().get("bogusfield").getFormat(), is("foo"));
        assertThat(result.getDataModel().getProperties().get("bogusfield").getType(), is("string"));
    }

    @Test
    public void ga4ghTypeWithoutAliasWorksWithColumnNameAsFirstArgument() throws IOException {
        SearchRequest query = new SearchRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') FROM %s", prestoPaginationTestTable));
        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().keySet(), contains("bogusfield"));
        assertThat(result.getDataModel().getProperties().get("bogusfield").getRef(), is("http://path/to/whatever.com"));
    }

    @Test
    public void ga4ghTypeWithRefAndAliasWithAsGivesBackRef() throws IOException {
        SearchRequest query = new SearchRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') as bf FROM %s", prestoPaginationTestTable));
        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().keySet(), contains("bf"));
        assertThat(result.getDataModel().getProperties().get("bf").getRef(), is("http://path/to/whatever.com"));
    }

    @Test
    public void ga4ghTypeWithRefAndAliasWithoutAsGivesBackRef() throws IOException {
        SearchRequest query = new SearchRequest(String.format("SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') bf FROM %s", prestoPaginationTestTable));
        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().keySet(), contains("bf"));
        assertThat(result.getDataModel().getProperties().get("bf").getRef(), is("http://path/to/whatever.com"));
    }

    @Test
    public void ga4ghTypeWithJsonRefAndAliasGivesBackJsonRef() throws IOException {
        SearchRequest query = new SearchRequest(String.format("SELECT ga4gh_type(bogusfield, '{\"$ref\":\"http://path/to/whatever.com\"}') as bf FROM %s", prestoPaginationTestTable));
        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);
        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().keySet(), contains("bf"));
        assertThat(result.getDataModel().getProperties().get("bf").getRef(), is("http://path/to/whatever.com"));
    }

    private void assertDatesAndTimesHaveCorrectValuesForZone(String zone, Map<String, String> expectedValues) throws IOException {
        SearchRequest query = new SearchRequest(String.format("SELECT * FROM " + prestoDateTimeTestTable + " WHERE zone='%s'", zone));
        log.info("Running query {}", query);

        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        result = searchApiGetAllPages(result);

        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query.getQuery() + ", but none were found.");
        } else if (result.getData().size() > 1) {
            throw new RuntimeException("Found more than one test table entry for " + zone + " time zone, but only one was expected.");
        }

        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));

        final Map<String, ColumnSchema> properties = result.getDataModel().getProperties();
        final Map<String, Object> row = result.getData().get(0);
        EXPECTED_FORMATS.entrySet().stream().forEach(entry -> {
            String columnName = entry.getKey();
            String expectedColumnFormat = entry.getValue();
            assertThat("Expected column with format " + expectedColumnFormat + " for column " + columnName + " (" + zone + " time zone)", properties.get(columnName).getFormat(), is(expectedColumnFormat));
            assertThat("Expected column with type string for column " + columnName + " (" + zone + " time zone)", properties.get(columnName).getType(), is("string"));
            assertThat("date/time/datetime column " + columnName + " had an unexpected value for zone "+zone, row.get(columnName), is(expectedValues.get(columnName)));
        });
    }

    @Test
    public void datesAndTimesHaveCorrectValuesForDatesAndTimesInsertedWithZone() throws IOException {
        for (Map.Entry<String, Map<String, String>> e : EXPECTED_VALUES.entrySet()) {
            log.info("Checking date and time was inserted correctly for zone " + e.getKey());
            assertDatesAndTimesHaveCorrectValuesForZone(e.getKey(), e.getValue());
        }
    }

    @Test
    public void indexIsPresentOnFirstPage() throws Exception {
        getFirstPageOfTableListing();
    }

    @Test
    public void nextPageTrailIsConsistentWithIndexOverFirst10Pages() throws Exception {
        final int MAX_PAGES_TO_TRAVERSE = 10;

        ListTableResponse currentPage = getFirstPageOfTableListing();

        List<PageIndexEntry> pageIndex = currentPage.getIndex();
        if (pageIndex.size() == 1) {
            assertThat(currentPage.getPagination(), is(nullValue()));
            return;
        }

        assertThat(currentPage.getPagination(), not(nullValue()));

        //assert that the nth page has next url equal to the n+1st index.
        for (int i = 1; i < Math.min(MAX_PAGES_TO_TRAVERSE, pageIndex.size() - 1); ++i) {
            log.info("Follow-up: Page {}: Start", i);
            currentPage = searchApiGetRequest(currentPage.getPagination().getNextPageUrl().toString(),
                200,
                ListTableResponse.class);
            log.info("Follow-up: Page {}: currentPage: {}", i, currentPage);
            //all pages with index < pageIndex.size() - 1 should have a non null valid next url.
            assertThat(currentPage.getPagination().getNextPageUrl(), not(nullValue()));
            if (i == (pageIndex.size() - 1)) {
                assertThat(currentPage.getPagination(), is(nullValue()));
            } else {
                assertThat(currentPage.getPagination().getNextPageUrl(), is(pageIndex.get(i + 1).getUrl()));
            }
            log.info("Follow-up: Page {}: End", i);
        }
        if (pageIndex.size() > MAX_PAGES_TO_TRAVERSE) {
            log.info("next page trail did not end after " + MAX_PAGES_TO_TRAVERSE + " requests, but was consistent with page index over that range.");
        }
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

        SearchRequest searchRequest = new SearchRequest(query);
        log.info("Running query {}", query);
        Table result = searchApiRequest(Method.POST, "/search", searchRequest, 200, Table.class);
        result = searchApiGetAllPages(result);

        if (result.getData() == null) {
            throw new RuntimeException("Expected results for query " + query + ", but none were found.");
        } else if (result.getDataModel() == null) {
            throw new RuntimeException("No data model was returned for query "+query);
        }
        return result;
    }

    @Test
    public void searchQueryOnVariedTypesReturnsCorrectData() throws Exception {
        Table result = executeSearchQueryOnVariedTypes();
        List<Map<String, Object>> expectedData;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("variedTypesData.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            expectedData = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>(){});
        }
        List<Map<String, Object>> actualData = result.getData();
        Assertions.assertThat(actualData).usingRecursiveComparison().isEqualTo(expectedData);
    }

    @Test
    public void searchQueryOnVariedTypesReturnsCorrectDataModel() throws Exception {

        Table result = executeSearchQueryOnVariedTypes();
        DataModel expectedDataModel;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("variedTypesDataModel.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            expectedDataModel = objectMapper.readValue(is, DataModel.class);
        }

        DataModel actualDataModel = result.getDataModel();
        Assertions.assertThat(actualDataModel).usingRecursiveComparison().isEqualTo(expectedDataModel);
    }

    @Test
    public void malformedSqlQueryShouldReturn400AndMessageAndTraceId() throws Exception {
        SearchRequest query = new SearchRequest("SELECT * FROM FROM E2ETEST LIMIT STRAWBERRY");
        Table data = searchUntilException(query, HttpStatus.SC_BAD_REQUEST);
        runBasicAssertionOnTableErrorList(data.getErrors());
        assertThat(data.getErrors().get(0).getStatus(), equalTo(400));
    }

    @Test
    public void sqlQueryWithBadColumnShouldReturn400AndMessageAndTraceId() throws Exception {
        SearchRequest query = new SearchRequest("SELECT e2etest_olywolypolywoly FROM " + prestoPaginationTestTable + " LIMIT 10");
        Table data = searchUntilException(query, HttpStatus.SC_BAD_REQUEST);
        runBasicAssertionOnTableErrorList(data.getErrors());
        assertThat(data.getErrors().get(0).getStatus(), equalTo(400));
    }

    @Test
    public void sqlQueryShouldFindSomething() throws Exception {

        SearchRequest query = new SearchRequest("SELECT * FROM " + prestoPaginationTestTable + " LIMIT 10");
        log.info("Running query {}", query);


        Table result = searchApiRequest(Method.POST, "/search", query, 200, Table.class);
        while (result.getPagination() != null) {
            result = searchApiGetRequest(result.getPagination().getNextPageUrl().toString(), 200, Table.class);
            if (result.getDataModel() != null) {
                break;
            }
        }

        assertThat(result, not(nullValue()));
        assertThat(result.getDataModel(), not(nullValue()));
        assertThat(result.getDataModel().getProperties(), not(nullValue()));
        assertThat(result.getDataModel().getProperties().entrySet(), hasSize(greaterThan(0)));
    }

    @Test
    public void getTables_should_returnAtLeastOneTable() throws Exception {
        ListTableResponse listTableResponse = searchApiGetRequest("/tables", 200, ListTableResponse.class);
        assertThat(listTableResponse, not(nullValue()));
        assertThat(listTableResponse.getTables(), hasSize(greaterThan(0)));
    }

    @Test
    public void getTableInfoWithUnknownCatalogGives404AndMessageAndTraceId() throws Exception {
        final String prestoTableWithBadCatalog = "e2etest_olywlypolywoly.public." + unqualifiedPaginationTestTable;
        TableInfo info = searchApiGetRequest("/table/" + prestoTableWithBadCatalog + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().get(0).getStatus(), equalTo(404));
    }

    @Test
    public void getTableInfoWithUnknownSchemaGives404AndMessageAndTraceId() throws Exception {
        final String prestoTableWithBadSchema = inMemoryCatalog + ".e2etest_olywolypolywoly." + unqualifiedPaginationTestTable;
        TableInfo info = searchApiGetRequest("/table/" + prestoTableWithBadSchema + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().get(0).getStatus(), equalTo(404));
    }

    @Test
    public void getTableInfoWithUnknownTableGives404AndMessageAndTraceId() throws Exception {
        final String prestoTableWithBadTable = inMemoryCatalog + "." + inMemorySchema + "." + "e2etest_olywolypolywoly";
        TableInfo info = searchApiGetRequest("/table/" + prestoTableWithBadTable + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().get(0).getStatus(), equalTo(404));
    }

    @Test
    public void getTableInfoWithBadlyQualifiedTableGives404AndMessageAndTraceId() throws Exception {
        final String prestoTableWithBadTable = "e2etest_olywolypolywoly";
        TableInfo info = searchApiGetRequest("/table/" + prestoTableWithBadTable + "/info", 404, TableInfo.class);
        runBasicAssertionOnTableErrorList(info.getErrors());
        assertThat(info.getErrors().get(0).getStatus(), equalTo(404));
    }

    @Test
    public void getTableInfo_should_returnTableAndSchema() throws Exception {
        Table tableInfo = searchApiGetRequest("/table/" + prestoPaginationTestTable + "/info", 200, Table.class);
        assertThat(tableInfo, not(nullValue()));
        assertThat(tableInfo.getName(), equalTo(prestoPaginationTestTable));
        assertThat(tableInfo.getDataModel(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getId(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getSchema(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getProperties(), not(nullValue()));
        assertThat(tableInfo.getDataModel().getProperties().entrySet(), not(empty()));
    }

    @Test
    public void getTableData_should_returnDataAndDataModel() throws Exception {
        Table tableData = searchApiGetRequest("/table/" + prestoPaginationTestTable + "/data", 200, Table.class);
        assertThat(tableData, not(nullValue()));
        tableData = searchApiGetAllPages(tableData);
        assertThat(tableData.getData(), not(nullValue()));
        assertThat(tableData.getData(), not(empty()));
        assertThat(tableData.getDataModel(), not(nullValue()));
        assertThat(tableData.getDataModel().getSchema(), not(nullValue()));
        assertThat(tableData.getDataModel().getProperties(), not(nullValue()));
        assertThat(tableData.getDataModel().getProperties().entrySet(), not(empty()));
    }

    @Test
    public void getTables_should_require_searchInfo_scope() throws Exception {
        assumeThat(globalMethodSecurityEnabled, is(true));
        assumeThat(scopeCheckingEnabled, is(true));

        givenAuthenticatedRequest("junk_scope")
            .when()
            .get("/tables")
            .then()
            .log().ifValidationFails()
            .statusCode(403)
            .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    @Test
    public void getTableData_should_require_searchData_scope() throws Exception {
        assumeThat(globalMethodSecurityEnabled, is(true));
        assumeThat(scopeCheckingEnabled, is(true));

        givenAuthenticatedRequest("junk_scope")
            .when()
            .get("/table/{tableName}/data", prestoPaginationTestTable)
            .then()
            .log().ifValidationFails()
            .statusCode(403)
            .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    @Test
    public void searchQuery_should_require_searchDataAndSearchQuery_scopes() throws Exception {
        assumeThat(globalMethodSecurityEnabled, is(true));
        assumeThat(scopeCheckingEnabled, is(true));

        SearchRequest testSearchRequest = new SearchRequest("SELECT * FROM E2ETEST LIMIT 10");
        givenAuthenticatedRequest("search:data") // but not search:query
                .when()
                .contentType(ContentType.JSON)
                .body(testSearchRequest)
                .post("/search")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));

        givenAuthenticatedRequest("search:query") // but not search:data
                .when()
                .contentType(ContentType.JSON)
                .body(testSearchRequest)
                .post("/search")
                .then()
                .log().ifValidationFails()
                .statusCode(403)
                .header("WWW-Authenticate", containsString("error=\"insufficient_scope\""));
    }

    static void runBasicAssertionOnTableErrorList(List<TableError> errors) {
        assertThat(errors, not(nullValue()));
        assertThat(errors.size(), equalTo(1));
        assertThat(errors.get(0).getTitle(), not(nullValue()));
        assertThat(errors.get(0).getDetails(), not(nullValue()));
    }

    /**
     * Retrieves all rows of the given table by following pagination links page by page.
     *
     * @param table the table with the initial row set and pagination link.
     * @return A table containing the concatenation of all rows returned over all the pagination links, as well as the
     * first non-null data model encountered.  null only if original table parameter is null.
     */
    static Table searchApiGetAllPages(Table table) throws IOException {
        while (table.getPagination() != null && table.getPagination().getNextPageUrl() != null) {
            String nextPageUri = table.getPagination().getNextPageUrl().toString();
            Table nextResult = searchApiGetRequest(nextPageUri, 200, Table.class);
            if (nextResult.getData() != null) {
                log.info("Got " + nextResult.getData().size() + " results");
            }
            table.append(nextResult);
        }
        return table;
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
     *                        well-formed Search API credentials challenges from the server, which are automatically retried).
     */
    static <T> T searchApiGetRequest(String path, int expectedStatus, Class<T> responseType) throws IOException {
        return searchApiRequest(GET, path, null, expectedStatus, responseType);
    }

    /**
     * Performs an HTTP request with the currently configured authentication settings (both bearer tokens and extra
     * credentials requested by the Search API within the current test method). GA4GH Search API credential challenges
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
    static <T> T searchApiRequest(Method method, String path, Object body, int expectedStatus, Class<T> responseType) throws IOException {
        if (expectedStatus == 401) {
            fail("This method handles auth challenges and retries on 401. You can't use it when you want a 401 response.");
        }

        return getResponse(method, path, body)
            .then()
            .log().ifValidationFails()
            .statusCode(expectedStatus)
            .extract()
            .as(responseType);
    }

    /**
     * Executes a search query and follows nextUri links until a response returns the HTTP error code in expectedErrorStatus.
     * If the expected status is never reached, an assertion error is thrown.
     *
     * @return UserFacingError The error object describing the expected error.
     * @throws IOException
     */
    private static Table searchUntilException(Object query, int expectedErrorStatus) throws IOException {
        Response response = getResponse(Method.POST, "/search", query);
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            log.info("Got status OK after POSTing search");
            Table table = response.then().log().ifValidationFails(LogDetail.ALL).extract().as(Table.class);
            while (table.getPagination() != null && table.getPagination().getNextPageUrl() != null) {
                String nextPageUri = table.getPagination().getNextPageUrl().toString();
                Response nextPageResponse = getResponse(Method.GET, nextPageUri, null);
                log.info("Looking for status " + expectedErrorStatus + " by following nextPageUri trail, most recent request returned " + nextPageResponse.getStatusCode());
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

    private static Response getResponse(Method method, String path, Object body) throws IOException {
        Optional<HttpAuthChallenge> wwwAuthenticate;
        for (int attempt = 0; attempt < MAX_REAUTH_ATTEMPTS; attempt++) {

            // this request includes all the extra credentials we have been challenged for so far
            String defaultScope = optionalEnv("E2E_WALLET_DEFAULT_SCOPE", null);
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
                if (!wwwAuthenticate.isPresent()) {
                    throw new AssertionError("Got HTTP 401 without WWW-Authenticate header");
                }

                if ("invalid_token".equals(wwwAuthenticate.get().getParams().get("error"))) {
                    log.info("Try running again with E2E_LOG_TOKENS=true to see what's wrong");
                }

                assertThat(wwwAuthenticate.get().getScheme(), is("GA4GH-Search"));

                SearchAuthChallengeBody challengeBody = response.as(SearchAuthChallengeBody.class);
                SearchAuthRequest searchAuthRequest = challengeBody.getAuthorizationRequest();

                assertAuthChallengeIsValid(wwwAuthenticate.get(), searchAuthRequest);
                String token = supplyCredential(searchAuthRequest);

                String existingCredential = extraCredentials.put(searchAuthRequest.getKey(), token);

                assertThat("Got re-challenged for the same credential " + searchAuthRequest + ". Is the token bad or expired?",
                    existingCredential, nullValue());
                continue;
            } else {
                return response;
            }
        }
        throw new AssertionError(
            "Exceeded MAX_REAUTH_ATTEMPTS (" + MAX_REAUTH_ATTEMPTS + ")." +
                " Tokens gathered so far: " + extraCredentials.keySet());
    }

    private static void assertAuthChallengeIsValid(HttpAuthChallenge wwwAuthenticate, SearchAuthRequest searchAuthRequest) {
        assertThat("Auth challenge body must contain an authorization-request but it was " + searchAuthRequest,
            searchAuthRequest, not(nullValue()));
        assertThat("Key must be present in auth request",
            searchAuthRequest.getKey(), not(nullValue()));
        assertThat("Key must match realm in auth challenge header",
            wwwAuthenticate.getParams().get("realm"), is(searchAuthRequest.getKey()));
        assertThat("Resource must be described in auth request",
            searchAuthRequest.getResourceDescription(), not(nullValue()));
    }

    private static String supplyCredential(SearchAuthRequest searchAuthRequest) throws IOException {
        log.info("Handling auth challenge {}", searchAuthRequest);

        // first check for a configured token
        // a real client wouldn't use the key to decide what to get; that would complect the client with catalog naming choices!
        // a real client should do a credential lookup using the type and resource-description!
        String tokenEnvName = "E2E_SEARCH_CREDENTIALS_" + searchAuthRequest.getKey().toUpperCase();
        String configuredToken = optionalEnv(tokenEnvName, null);
        if (configuredToken != null) {
            log.info("Using {} to satisfy auth challenge", tokenEnvName);
            return configuredToken;
        }

        if (searchAuthRequest.getResourceType().equals("bigquery")) {
            log.info("Using Google Application Default credentials to satisfy auth challenge");
            return getGoogleCredentials().getAccessToken().getTokenValue();
        }

        throw new RuntimeException("Can't satisfy auth challenge " + searchAuthRequest + ": unknown resource type. Try defining " + tokenEnvName + ".");
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
        RequestSpecification req = given()
            .config(config);

        // add auth if configured
        if (walletClientId != null && walletClientSecret != null && searchAdapterAudience != null) {
            String accessToken = getToken(searchAdapterAudience, scopes);
            req.auth().oauth2(accessToken);
            if (Boolean.parseBoolean(optionalEnv("E2E_LOG_TOKENS", "false"))) {
                log.info("Using access token {}", accessToken);
            }
        }

        // add extra credentials
        extraCredentials.forEach((k, v) -> req.header("GA4GH-Search-Authorization", k + "=" + v));

        return req;
    }

    static String discoverTableName() throws IOException {
        ListTableResponse listTableResponse = searchApiGetRequest("/tables", 200, ListTableResponse.class);
        assertThat(listTableResponse.getTables(), hasSize(greaterThan(0)));
        return listTableResponse.getTables().get(0).getName();
    }

}
