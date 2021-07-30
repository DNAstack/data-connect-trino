package com.dnastack.ga4gh.search.adapter.presto;

import brave.Tracer;
import brave.propagation.TraceContext;
import com.dnastack.audit.logger.AuditEventLogger;
import com.dnastack.audit.model.*;
import com.dnastack.ga4gh.search.ApplicationConfig;
import com.dnastack.ga4gh.search.DataModelSupplier;
import com.dnastack.ga4gh.search.client.tablesregistry.OAuthClientConfig;
import com.dnastack.ga4gh.search.client.tablesregistry.TablesRegistryClient;
import com.dnastack.ga4gh.search.adapter.presto.exception.*;
import com.dnastack.ga4gh.search.repository.QueryJob;
import com.dnastack.ga4gh.search.repository.QueryJobRepository;
import com.dnastack.ga4gh.search.model.ColumnSchema;
import com.dnastack.ga4gh.search.model.DataModel;
import com.dnastack.ga4gh.search.model.PageIndexEntry;
import com.dnastack.ga4gh.search.model.Pagination;
import com.dnastack.ga4gh.search.model.TableData;
import com.dnastack.ga4gh.search.model.TableInfo;
import com.dnastack.ga4gh.search.model.TablesList;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.annotation.Obsolete;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Configuration
public class PrestoSearchAdapter {
    private static final String NEXT_PAGE_SEARCH_TEMPLATE = "/search/%s"; //todo: alternatives?
    private static final String NEXT_PAGE_CATALOG_TEMPLATE = "/tables/catalog/%s";
    private static final URI JSON_SCHEMA_DRAFT7_URI = URI.create("http://json-schema.org/draft-07/schema#");

    //Matches the given name against the pattern <catalog>.<schema>.<table>, "<catalog>"."<schema>"."<table>", or
    //"<catalog>.<schema>.<table>".  Note this pattern is permissive and will often allow misquoted names through.
    private static final Pattern qualifiedNameMatcher =
            Pattern.compile("^\"?[^\"]+\"?\\.\"?[^\"]+\"?\\.\"?[^\"]+\"?$");

    @Autowired
    private PrestoClient client;

    @Autowired
    private ThrowableTransformer throwableTransformer;

    @Autowired
    private QueryJobRepository queryJobRepository;

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private AuditEventLogger auditEventLogger;

    @Autowired(required = false)
    private DataModelSupplier dataModelSupplier;

    @Autowired
    private Tracer tracer;

    private boolean hasMore(TableData tableData) {
        if (tableData.getPagination() != null && tableData.getPagination().getNextPageUrl() != null) {
            return true;
        }
        return false;
    }

    // Pattern to match ga4gh_type two argument function
    static final Pattern biFunctionPattern = Pattern.compile("((ga4gh_type)\\(\\s*([^,]+)\\s*,\\s*('[^']+')\\s*\\)((\\s+as)?\\s+((?!FROM\\s+)[A-Za-z0-9_]*))?)", Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    @Getter
    static class SQLFunction {
        final String functionName;
        final List<String> args = new ArrayList<>();
        final String columnAlias;

        public String getFunctionName() {
            return functionName;
        }
        public String getColumnAlias() {
            return columnAlias;
        }
        public List<String> getArgs() {
            return args;
        }
        public SQLFunction(MatchResult matchResult) {
            this.functionName = matchResult.group(2);
            for(int i = 3; i < matchResult.groupCount()-2; ++i) {
                this.args.add(matchResult.group(i));
            }
            this.columnAlias = matchResult.group(matchResult.groupCount());
            log.debug("Extracted function "+this.functionName+" with alias "+((columnAlias!=null) ? columnAlias : "null"));
        }

    }

    //rewrites the query by replacing all instances of functionName(a_0, a_1)
    //with a_argIndex
    private String rewriteQuery(String query, String functionName, int argIndex) {
        return biFunctionPattern.matcher(query)
                                .replaceAll(matchResult-> {
                           SQLFunction sf = new SQLFunction(matchResult);
                           if (sf.getFunctionName().equals(functionName)) {
                               String col = sf.getArgs().get(argIndex);
                               String alias = sf.getColumnAlias();
                               return (alias == null) ? col : col+" as "+alias;
                           }
                          return matchResult.group(1); //pass function through unchanged.
                       });
    }

    // Extracts all two-argument SQL functions from a query.
    private Stream<SQLFunction> parseSQLBiFunctions(String query) {
        Matcher matcher = biFunctionPattern.matcher(query);
        return matcher.results().map(SQLFunction::new);
    }

    // Given the parsed representation of the ga4gh_type function,
    // returns the type (second argument of the function), without quotes.
    // (this will be a JSON schema, or the shorthand $ref:<URL>)
    private String getGa4ghType(SQLFunction ga4ghFunction) {
        String ga4ghType = ga4ghFunction.getArgs().get(1).strip();
        if ((ga4ghType.startsWith("'") && ga4ghType.endsWith("'")) ||
           (ga4ghType.startsWith("\"") && ga4ghType.endsWith("\""))) {
            return ga4ghType.substring(1, ga4ghType.length()-1);
        } else {
            throw new QueryParsingException("Couldn't parse query: second argument to ga4gh_type must be quoted.");
        }
    }

    // Given tableData representing some search result, applies "type casting" of the result as described by the given
    // parsed ga4gh_type function.
    private void applyGa4ghTypeSqlFunction(SQLFunction ga4ghTypeFunction, TableData tableData) {
        ObjectMapper objectMapper = new ObjectMapper();
        DataModel dataModel = tableData.getDataModel();
        if (dataModel ==  null) {
            return;
        }

        if (dataModel.getRef() != null) {
            //sanity check
            throw new RuntimeException("Unable to apply SQL function to response with indirect $ref");
        }

        Map<String, ColumnSchema> columnSchemaMap = new HashMap<>(dataModel.getProperties());

        String columnName = (ga4ghTypeFunction.getColumnAlias()) != null ? ga4ghTypeFunction.getColumnAlias() : ga4ghTypeFunction.getArgs().get(0);
        String ga4ghType = getGa4ghType(ga4ghTypeFunction);

        ColumnSchema newColumnSchema;
        if (ga4ghType.startsWith("$ref:")) {
            String[] parts = ga4ghType.split(":",2);
            if (parts.length != 2) {
                //This could have been detected earlier, but whatever.
                throw new QueryParsingException("Unexpected second argument to ga4gh_type function, must be a valid JSON schema or the $ref:<URL> shorthand");
            }
            newColumnSchema = ColumnSchema.builder()
                    .ref(parts[1])
                    .build();
        } else {
            try {
                newColumnSchema = objectMapper.readValue(ga4ghType, ColumnSchema.class);
            } catch (IOException e) {
                throw new QueryParsingException("Unexpected second argument to ga4gh_type function, must be a valid JSON schema or the $ref:<URL> shorthand.", e);
            }
        }

        ColumnSchema columnSchema = columnSchemaMap.get(columnName);
        if (columnSchema == null) {
            throw new QueryParsingException("ga4gh_type was applied to column "+columnName+", but this column was not found in response.");
        } else {
            columnSchemaMap.put(columnName, newColumnSchema);
        }
        dataModel.setProperties(columnSchemaMap);
    }

    // Perform the given query and gather ALL results, by following Presto's nextUrl links
    // The query should NOT contain any functions that would not be recognized by Presto.
    public TableData searchAll(String statement,
                               HttpServletRequest request,
                               Map<String, String> extraCredentials,
                               DataModel dataModel) {
        log.debug("searchAll: Query: {}", statement);
        TableData tableData = search(statement, request, extraCredentials, dataModel);
        while (hasMore(tableData)) {
            log.debug("searchAll: Autoloading next page of data");
            TableData nextPage = getNextSearchPage(tableData.getPagination().getPrestoNextPageUrl().getPath(), null, request, extraCredentials);
            log.debug("searchAll: nextPage.size(): {}", nextPage.getData().size());
            tableData.append(nextPage);
        }

        if (tableData.getDataModel() == null) {
            throw new DataModelNotDefinedException("The data model cannot be determined.");
        } else if (!tableData.getDataModel().isUsable()) {
            throw new DataModelNotDefinedException("The data model is not usable.");
        }

        return tableData;
    }

    // Perform the given query and gather ALL results, by following Presto's nextUrl links
    // The query should NOT contain any functions that would not be recognized by Presto.
    public TableData searchUntilHavingFirstRow(String statement,
                                               HttpServletRequest request,
                                               Map<String, String> extraCredentials,
                                               DataModel dataModel) {
        log.debug("searchUntilHavingFirstRow: Query: {}", statement);
        TableData tableData = search(statement, request, extraCredentials, dataModel);
        while (hasMore(tableData)) {
            log.debug("searchUntilHavingFirstRow: Autoloading next page of data");
            TableData nextPage = getNextSearchPage(tableData.getPagination().getPrestoNextPageUrl().getPath(), null, request, extraCredentials);
            log.debug("searchUntilHavingFirstRow: nextPage.size(): {}", nextPage.getData().size());
            tableData.append(nextPage);
            if (!nextPage.getData().isEmpty()) {
                break;
            }
        }

        if (tableData.getDataModel() == null) {
            throw new DataModelNotDefinedException("The data model cannot be determined.");
        } else if (!tableData.getDataModel().isUsable()) {
            throw new DataModelNotDefinedException("The data model is not usable.");
        }

        return tableData;
    }

    public TableData search(String query,
                            HttpServletRequest request,
                            Map<String, String> extraCredentials,
                            DataModel dataModel) {

        log.info("Received query: " + query + ".");
        String rewrittenQuery = rewriteQuery(query, "ga4gh_type", 0);
        logAuditEvent(request, "search", "query", Map.of(
            "query", Optional.ofNullable(rewrittenQuery).orElse("(undefined)")
        ));
        JsonNode response = client.query(rewrittenQuery, extraCredentials);
        QueryJob queryJob = createQueryJob(query, dataModel);
        TableData tableData = toTableData(NEXT_PAGE_SEARCH_TEMPLATE, response, queryJob.getId(), request);
        return tableData;
    }

    public TableData getNextSearchPage(String page, String queryJobId, HttpServletRequest request, Map<String, String> extraCredentials) {
        logAuditEvent(request, "search", "next-page", Map.of(
            "page", Optional.ofNullable(page).orElse("(undefined)")
        ));
        JsonNode response = client.next(page, extraCredentials);
        TableData tableData = toTableData(NEXT_PAGE_SEARCH_TEMPLATE, response, queryJobId, request);
        populateTableSchemaIfAvailable(queryJobId, tableData);
        return tableData;
    }

    private QueryJob createQueryJob(String query, DataModel dataModel) {

        String tableSchema = null;
        if (dataModel != null) {
            try {
                tableSchema = new ObjectMapper().writeValueAsString(dataModel);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Error while parsing table schema.", ex);
            }
        }

        QueryJob queryJob = QueryJob.builder()
                .query(query)
                .id(UUID.randomUUID().toString())
                .schema(tableSchema)
                .build();
        return queryJobRepository.save(queryJob);
    }

    private URI getLinkToCatalog(String catalog, HttpServletRequest request) {
        return URI.create(callbackBaseUrl(request) + String.format(NEXT_PAGE_CATALOG_TEMPLATE, catalog));
    }

    private PageIndexEntry getPageIndexEntryForCatalog(String catalog, int page, HttpServletRequest request) {
        URI uri = getLinkToCatalog(catalog, request);
        return PageIndexEntry.builder()
                             .catalog(catalog)
                             .url(uri)
                             .page(page)
                             .build();
    }

    private List<PageIndexEntry> getPageIndex(Set<String> catalogs, HttpServletRequest request) {
        final int[] page = {0};
        return catalogs.stream().map(catalog->getPageIndexEntryForCatalog(catalog, page[0]++, request)).collect(Collectors.toList());
    }

    private TablesList getTables(String currentCatalog, String nextCatalog, HttpServletRequest request, Map<String, String> extraCredentials) {
        PrestoCatalog prestoCatalog = new PrestoCatalog(this, throwableTransformer, callbackBaseUrl(request), currentCatalog);
        Pagination nextPage = null;
        if (nextCatalog != null) {
            nextPage = new Pagination(null, getLinkToCatalog(nextCatalog, request), null);
        }

        TablesList tablesList = prestoCatalog.getTablesList(nextPage, request, extraCredentials);

        return tablesList;
    }

    public TablesList getTables(HttpServletRequest request, Map<String, String> extraCredentials) {
        logAuditEvent(request, "table", "read", null);
        Set<String> catalogs = getPrestoCatalogs(request, extraCredentials);
        if (catalogs == null || catalogs.isEmpty()) {
            return new TablesList(List.of(), null, null);
        }
        Iterator<String> catalogIt = catalogs.iterator();

        TablesList tablesList = getTables(catalogIt.next(), catalogIt.hasNext() ? catalogIt.next() : null, request, extraCredentials);
        tablesList.setIndex(getPageIndex(catalogs, request));
        return tablesList;
    }

    public TablesList getTablesInCatalog(String catalog, HttpServletRequest request, Map<String, String> extraCredentials) {
        logAuditEvent(request, "table", "in-catalog", Map.of(
            "catalog", Optional.ofNullable(catalog).orElse("(undefined)")
        ));
        Set<String> catalogs = getPrestoCatalogs(request, extraCredentials);
        if (catalogs != null) {
            Iterator<String> catalogIt = catalogs.iterator();
            while (catalogIt.hasNext()) {
                if (catalogIt.next().equals(catalog)) {
                    return getTables(catalog, catalogIt.hasNext() ? catalogIt.next() : null, request, extraCredentials);
                }
            }
        }
        throw new PrestoNoSuchCatalogException("No such catalog " + catalog);
    }

    private static String quote(String sqlIdentifier) {
        return "\"" + sqlIdentifier.replace("\"", "\"\"") + "\"";
    }

    public TableData getTableData(String tableName, HttpServletRequest request, Map<String, String> extraCredentials) {
        logAuditEvent(request, "table", "data", Map.of(
            "table", Optional.ofNullable(tableName).orElse("(undefined)")
        ));
        // Get table JSON schema from tables registry if one exists for this table (for tables from presto-public)
        DataModel dataModel = getDataModelFromSupplier(tableName);
        //Add quotes to tableName in the query. Table name can be of the format <catalog_name>.<datasource_name>.tableName
        //So if the tableName has two dots in it, then everything after the third dot, should come within quotes.
        String validTableName = getTableNameInCorrectFormat(tableName);
        TableData tableData = search("SELECT * FROM " + validTableName, request, extraCredentials, dataModel);

        // Populate the dataModel only if there is tableData
        if (!tableData.getData().isEmpty()) {

            // If the dataModel is not available from tables-registry, use the one from tableData
            // Fill in the id & comments if the data model is ready
            if (dataModel == null && tableData.getDataModel() != null) {
                dataModel = tableData.getDataModel();
                dataModel.setId(getDataModelId(tableName, request));
                attachCommentsToDataModel(dataModel, tableName, request, extraCredentials);
            } else {
                tableData.setDataModel(dataModel);
            }
        }
        return tableData;
    }

    public TableInfo getTableInfo(String tableName,
                                  HttpServletRequest request,
                                  Map<String, String> extraCredentials) {
        logAuditEvent(request, "table", "info", Map.of(
            "table", Optional.ofNullable(tableName).orElse("(undefined)")
        ));
        if (!isValidPrestoName(tableName)) {
            //triggers a 404.
            throw new PrestoBadlyQualifiedNameException("Invalid tablename "+tableName+" -- expected name in format <catalog>.<schema>.<tableName>");
        }

        // Get table JSON schema from tables registry if one exists for this table (for tables from presto-public)
        DataModel dataModel = getDataModelFromSupplier(tableName);

        //Add quotes to tableName in the query. Table name can be of the format <catalog_name>.<datasource_name>.tableName
        //So if the tableName has two dots in it, then everything after the third dot, should come within quotes.
        String validTableName = getTableNameInCorrectFormat(tableName);
        TableData tableData = searchAll("SELECT * FROM " + validTableName + " LIMIT 1", request, extraCredentials, dataModel);

        // If the dataModel is not available from tables-registry, use the one from tableData
        // Fill in the id & comments if the data model is ready
        if (dataModel == null && tableData.getDataModel() != null) {
            dataModel = tableData.getDataModel();
            dataModel.setId(getDataModelId(tableName, request));
        }

        return new TableInfo(tableName, dataModel.getDescription(), dataModel, null);
    }

    private String getTableNameInCorrectFormat(String tableName) {
        String validTableName = tableName;
        if (StringUtils.countMatches(tableName, ".") >= 2 ) {

            // If there are two or more dots, then quote the entire part after the second dot(assuming that this will be the table name).
            int secondIndex = StringUtils.ordinalIndexOf(tableName, ".", 2);

            //Everything before second catalog name will be catalog(+schema)
            String catalogAndSchema = tableName.substring(0, secondIndex + 1);
            String table = tableName.substring(secondIndex + 1);

            //If the table name doesn't starts with or ends with quotes then add quotes
            if (!table.startsWith("\"") || !table.endsWith("\"")) {
                table = "\"" + table + "\"";
            }
            validTableName = catalogAndSchema + table;
        } else {
            log.warn("Table name {} has less than 2 dots in it.", tableName);
        }
        return validTableName;
    }

    private boolean isValidPrestoName(String tableName) {
        return qualifiedNameMatcher.matcher(tableName).matches();
    }

    private TableData toTableData(String nextPageTemplate,
                                  JsonNode prestoResponse,
                                  String queryJobId,
                                  HttpServletRequest request) {
        List<Map<String, Object>> data = new ArrayList<>();
        DataModel dataModel = null;
        if (prestoResponse.hasNonNull("columns")) {
            final JsonNode columns = prestoResponse.get("columns");
            dataModel = generateDataModel(columns);
            if (prestoResponse.hasNonNull("data")) {
                for (JsonNode dataNode : prestoResponse.get("data")) { //for each row
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    int i = 0;
                    for (Map.Entry<String, ColumnSchema> entry : dataModel.getProperties().entrySet()) {
                        rowData.put(entry.getKey(), getData(entry.getValue(), dataNode.get(i++)));
                    }
                    data.add(rowData);
                }
            }
        } else if (prestoResponse.hasNonNull("error")) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                PrestoError prestoError = objectMapper.readValue(prestoResponse.get("error").toString(), PrestoError.class);
                log.error("Presto Error: State: {}", prestoResponse.get("stats").get("state"));
                log.error("Presto Error: Message: {}", prestoResponse.get("error").get("message"));
                log.error(
                    "Presto Error: Error: {} ({}): {}",
                    prestoResponse.get("error").get("errorType"),
                    prestoResponse.get("error").get("errorCode"),
                    prestoResponse.get("error").get("errorName")
                );

                final var stack = new ArrayList<String>();
                for (var it = prestoResponse.get("error").get("failureInfo").get("stack").iterator(); it.hasNext();) {
                    stack.add(it.next().asText());
                }

                log.error(
                    "Presto Error: Original Trace: {}: {}\n\tat {}",
                    prestoResponse.get("error").get("failureInfo").get("type"),
                    prestoResponse.get("error").get("failureInfo").get("message"),
                    String.join("\n\tat ", stack)
                );

                log.error("\n\tat {}", String.join("\n\tat ", stack));
                if (prestoError.getErrorName().equals("CATALOG_NOT_FOUND")) {
                    throw new PrestoNoSuchCatalogException(prestoError);
                } else if (prestoError.getErrorName().equals("SCHEMA_NOT_FOUND")) {
                    throw new PrestoNoSuchSchemaException(prestoError);
                } else if (prestoError.getErrorName().equals("TABLE_NOT_FOUND")) {
                    throw new PrestoNoSuchTableException(prestoError);
                } else if (prestoError.getErrorName().equals("COLUMN_NOT_FOUND")) {
                    throw new PrestoNoSuchColumnException(prestoError);
                } else if (prestoError.getErrorType().equals("USER_ERROR")) {
                    //Most other USER_ERRORs are bad queries and should likely return BAD_REQUEST error code.
                    throw new PrestoInvalidQueryException(prestoError);
                } else if (prestoError.getErrorType().equals("INSUFFICIENT_RESOURCES")) {
                    throw new PrestoInsufficientResourcesException(prestoError);
                } else {
                    // as of this commit, the remaining presto error type is 'internal error', but this
                    // will also be a catch all.
                    throw new PrestoInternalErrorException(prestoError);
                }
            } catch (IOException ex) {
                throw new UncheckedTableDataConstructionException(ex);
            }

        }

        // Generate pagination
        Pagination pagination = generatePagination(nextPageTemplate, prestoResponse, queryJobId, request);

        TableData tableData = new TableData(dataModel, Collections.unmodifiableList(data), null, pagination);
        if (queryJobId != null) {
            applyResponseTransforms(queryJobId, tableData);
        }

        if (tableData.getData().isEmpty()) {
            log.warn("No data listed");
        } else {
            if (tableData.getDataModel() == null) {
                throw new DataModelNotDefinedException("The data model is null as it cannot be derived from Presto.");
            } else if (!tableData.getDataModel().isUsable()) {
                throw new DataModelNotDefinedException("The data model is not usable. (object = " + tableData.getDataModel() + ")");
            }
        }

        return tableData;
    }

    // Parses the saved query identified by queryJobId, finds all functions that transform the response in some way,
    // and applies them to the response represented by tableData.
    private void applyResponseTransforms(String queryJobId, final TableData tableData) {
        QueryJob queryJob = queryJobRepository
                .findById(queryJobId)
                .orElseThrow(()->new InvalidQueryJobException(queryJobId, "The query corresponding to this search could not be located."));
        String query = queryJob.getQuery();
        Stream<SQLFunction> responseTransformingFunctions = parseSQLBiFunctions(query);

        responseTransformingFunctions
                .filter(sqlFunction->sqlFunction.functionName.equals("ga4gh_type"))
                .forEach(sqlFunction-> applyGa4ghTypeSqlFunction(sqlFunction, tableData));
    }

    @SneakyThrows
    private Pagination generatePagination(String template, JsonNode prestoResponse, String queryJobId, HttpServletRequest request) {
        URI nextPageUri = null;
        URI prestoNextPageUri = null;
        if (prestoResponse.hasNonNull("nextUri")) {
            final String rawPrestoResponseUri = prestoResponse.get("nextUri").asText();
            final String rawPrestoRelayedPath = URI.create(rawPrestoResponseUri).getPath().replaceFirst("^/+", "");
            final String localForwardedPath = String.format(template, rawPrestoRelayedPath);

            nextPageUri = URI.create(callbackBaseUrl(request) + localForwardedPath);
            prestoNextPageUri = ServletUriComponentsBuilder.fromHttpUrl(rawPrestoResponseUri).build().toUri();
        }

        return new Pagination(queryJobId, nextPageUri, prestoNextPageUri);
    }

    /**
     * Returns the absolute base URL that the original caller (who may be behind an HTTP proxy) should use to reach
     * the root resource of this server. The returned URL string will be of the form {@code https://example.com:1234/forwarded/prefix}.
     * It will always have a protocol and host. It will have a port if the port is not the default for the protocol.
     * It may or may not have a path (depending on X-Forwarded-Prefix) and it will never end with a slash.
     *
     * @param request
     * @return
     */
    private String callbackBaseUrl(HttpServletRequest request) {

        if (log.isDebugEnabled()) {
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                if (List.of("authorization", "cookie").contains(headerName.toLowerCase())) {
                    log.debug("callbackBaseUrl: Request Header: {} => [obscured]", headerName);
                } else {
                    log.debug("callbackBaseUrl: Request Header: {} => [{}]", headerName, request.getHeader(headerName));
                }
            });
        }

        final var forwardedProtocol = request.getHeader("X-Forwarded-Proto");
        final var forwardedHost = request.getHeader("X-Forwarded-Host");
        final var forwardedPort = request.getHeader("X-Forwarded-Port");
        final var forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
        log.info("Forwarded headers: protocol={}, host={}, port={}, prefix={}", forwardedProtocol, forwardedHost, forwardedPort, forwardedPrefix);

        ServletUriComponentsBuilder urlBuilder = ServletUriComponentsBuilder.fromContextPath(request);
        if (forwardedProtocol != null) {
            urlBuilder.scheme(forwardedProtocol);
        }
        if (forwardedHost != null) {
            urlBuilder.host(forwardedHost);
        }
        if (forwardedPort != null) {
            // we need to eliminate the default port numbers because of Wallet pickiness
            String scheme = urlBuilder.build().getScheme();
            if ((scheme.equals("https") && !forwardedPort.equals("443")) ||
                    (scheme.equals("http") && !forwardedPort.equals("80"))) {
                urlBuilder.port(Integer.parseInt(forwardedPort));
            }
        }
        if (forwardedPrefix != null) {
            urlBuilder.path(stripTrailingSlashes(forwardedPrefix));
        }

        String result = urlBuilder.build().toUriString();
        log.info("Final callback URL: " + result);
        return result;
    }

    private static String stripTrailingSlashes(String str) {
        while (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private static <T, K, U> Collector<T, ?, Map<K,U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
                                                                   Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper,
                         valueMapper,
                         (k,v)->{ throw new UnexpectedQueryResponseException("Duplicate key "+k); },
                         LinkedHashMap::new);
    }


    private Object getData(ColumnSchema columnSchema, JsonNode prestoDataArray) {
        if (columnSchema.getRawType().equals("map")) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            if (prestoDataArray.getNodeType() != JsonNodeType.OBJECT) {
                throw new UnexpectedQueryResponseException("Expected value for map was not of type object for schema "+columnSchema);
            }

            ColumnSchema mapEntryColumnSchema = columnSchema.getProperties().get("value");
            return Streams.stream(prestoDataArray.fields())
                   .map(mapEntry-> Map.entry(mapEntry.getKey(), getData(mapEntryColumnSchema, mapEntry.getValue())))
                   .collect(toSortedMap(deepMapEntry->deepMapEntry.getKey(), deepMapEntry->deepMapEntry.getValue()));
        } else if (columnSchema.getRawType().equals("row")) {
            if (prestoDataArray.getNodeType() != JsonNodeType.ARRAY) {
                throw new UnexpectedQueryResponseException("Expected array of row values for schema " + columnSchema);
            }
            int j = 0;
            Map<String, Object> row = new LinkedHashMap<>();
            for (Map.Entry<String, ColumnSchema> rowPropertyTypeInfo : columnSchema.getProperties().entrySet()) {
                JsonNode rowValue = prestoDataArray.get(j++);
                row.put(rowPropertyTypeInfo.getKey(), getData(rowPropertyTypeInfo.getValue(), rowValue));
            }
            return row;
        } else if (columnSchema.getRawType().equals("array")) {
            if (prestoDataArray.getNodeType() != JsonNodeType.ARRAY) {
                throw new UnexpectedQueryResponseException("Expected array of row values for schema " + columnSchema);
            }
            ColumnSchema itemSchema = columnSchema.getItems();
            return StreamSupport.stream(prestoDataArray.spliterator(), false)
                         .map(arrayValue->getData(itemSchema, arrayValue))
                         .collect(Collectors.toUnmodifiableList());
        } else if (columnSchema.getRawType() == "json") { //json or primitive.
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
                return objectMapper.readValue(prestoDataArray.asText(), new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new UnexpectedQueryResponseException("JSON came back badly formatted: " + prestoDataArray.asText());
            }
        } else {

            if (prestoDataArray.isTextual()) {
                //currently only textual types are transformed.
                PrestoDataTransformer transformer =  JsonAdapter.getPrestoDataTransformer(columnSchema.getRawType());
                if (transformer == null) {
                    return prestoDataArray.asText();
                } else{
                    return transformer.transform(prestoDataArray.asText());
                }
            } else if (prestoDataArray.isBoolean()) {
                return prestoDataArray.asBoolean();
            } else if (prestoDataArray.isIntegralNumber()) {
                return prestoDataArray.asLong();
            } else if (prestoDataArray.isFloatingPointNumber()) {
                return prestoDataArray.asDouble();
            } else if (prestoDataArray.isNull()) {
                return null;
            } else {
                throw new UnexpectedQueryResponseException("Unexpected value type in data for schema "+columnSchema);
            }
        }
    }

    private ColumnSchema getColumnSchema(JsonNode prestoTypeDescription) {
        final String rawType = prestoTypeDescription.get("rawType").asText();
        final String format = JsonAdapter.toFormat(prestoTypeDescription.get("rawType").asText());
        if (rawType.equalsIgnoreCase("array")) {
            ColumnSchema columnSchema = getColumnSchema(prestoTypeDescription.get("arguments").get(0).get("value"));
            return ColumnSchema.builder()
                    .type("array")
                    .rawType(rawType)
                    .comment("array["+columnSchema.getType()+"]")
                    .items(columnSchema)
                    .build();
        } else if (rawType.equalsIgnoreCase("row")) {
            JsonNode args = prestoTypeDescription.get("arguments");

            Map<String, ColumnSchema> m = StreamSupport.stream(args.spliterator(), false)
                         .collect(
                                 Collectors.toMap(rowArg->rowArg.get("value").get("fieldName").get("name").asText(),
                                                  rowArg->getColumnSchema(rowArg.get("value").get("typeSignature")),
                                                  (k,v)->{ throw new UnexpectedQueryResponseException("rows must have unique key names. Duplicate key "+k+", value="+v); },
                                                  LinkedHashMap::new)); //maintain key order to generate better comment.


            return ColumnSchema.builder()
                    .type("object")
                    .rawType(rawType)
                    .comment(String.format("row(%s)", Strings.join(
                            m.values().stream()
                             .map(cs->cs.getType())
                             .collect(Collectors.toList()), ',')))
                    .properties(m)
                    .build();
        } else if (rawType.equalsIgnoreCase("map")) {

            ColumnSchema keySchema = getColumnSchema(prestoTypeDescription.get("arguments").get(0).get("value"));
            ColumnSchema valueSchema = getColumnSchema(prestoTypeDescription.get("arguments").get(1).get("value"));

            return ColumnSchema.builder()
                    .type("object")
                    .rawType(rawType)
                    .comment(String.format("map(%s, %s)", keySchema.getType(), valueSchema.getType()))
                    .properties(Map.of("key", keySchema, "value", valueSchema))
                    .build();

        } else if (rawType.equalsIgnoreCase("json")) {
            return ColumnSchema.builder()
                               .type("object")
                               .rawType(rawType)
                               .comment("json")
                               .build();
        } else {
            //must be a primitive.
            String type = JsonAdapter.toJsonType(rawType);
            return ColumnSchema.builder()
                    .type(type)
                    .rawType(rawType)
                    .comment(rawType)
                    .format(format)
                    .build();
        }

    }

    private Map<String, ColumnSchema> getJsonSchemaProperties(JsonNode columns) {

        return StreamSupport.stream(columns.spliterator(), false)
                     .map(column->{
                         return Map.entry(column.get("name").asText(), getColumnSchema(column.get("typeSignature")));
                     }).collect(toSortedMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    /**
     * Get a list of the catalogs served by the connected instance of PrestoSQL.
     *
     * @return A List of Strings, where each String is the name of the catalog.
     * @throws IOException If the query to enumerate the list of catalogs fails.
     */
    private Set<String> getPrestoCatalogs(HttpServletRequest request, Map<String, String> extraCredentials) {
        TableData catalogs = searchAll("select catalog_name FROM system.metadata.catalogs ORDER BY catalog_name", request, extraCredentials, null);
        Set<String> catalogSet = new LinkedHashSet<>();
        for (Map<String, Object> row : catalogs.getData()) {
            String catalog = (String) row.get("catalog_name");
            if (applicationConfig.getHiddenCatalogs().contains(catalog.toLowerCase())) {
                log.debug("Ignoring catalog {}", catalog);
                continue;
            }

            log.trace("Found catalog {}", catalog);
            if (catalogSet.contains(catalog)) {
                throw new AssertionError("Unexpected duplicate catalog "+catalog);
            }
            catalogSet.add(catalog);
        }
        return catalogSet;
    }

    // DataModel related methods
    private DataModel generateDataModel(JsonNode columns) {
        return DataModel.builder()
                .id(null)
                .description("Automatically generated schema")
                .schema(JSON_SCHEMA_DRAFT7_URI)
                .properties(getJsonSchemaProperties(columns))
                .build();
    }

    private void attachCommentsToDataModel(DataModel dataModel,
                                           String tableName,
                                           HttpServletRequest request,
                                           Map<String, String> extraCredentials) {
        if (dataModel == null) {
            return;
        }

        Map<String, ColumnSchema> dataModelProperties = dataModel.getProperties();

        if (dataModelProperties == null) {
            return;
        }

        TableData describeData = searchAll("DESCRIBE " + tableName, request, extraCredentials, null);

        for (Map<String, Object> describeRow : describeData.getData()) {
            final String columnName = (String) describeRow.get("Column");
            final String comment = (String) describeRow.get("Comment");

            if (dataModelProperties.containsKey(columnName) && comment != null && !comment.isBlank()) {
                dataModelProperties.get(columnName).setComment(comment);
            }
        }
    }

    private URI getDataModelId(String tableName, HttpServletRequest request) {
        String refHost = callbackBaseUrl(request);
        return URI.create(String.format("%s/table/%s/info", refHost, tableName));
    }

    private DataModel getDataModelFromSupplier(String tableName) {
        if (dataModelSupplier == null) {
            return null;
        }

        return dataModelSupplier.supply(tableName);
    }

    private void populateTableSchemaIfAvailable(String queryJobId, TableData tableData) {

        if (!tableData.getData().isEmpty()) {
            // Retrieve the table schema stored in query_job table - the one fetched from tables-registry
            // Use this table schema to populate the dataModel
            DataModel dataModel = null;
            if (queryJobId != null) {
                QueryJob queryJob = queryJobRepository
                        .findById(queryJobId)
                        .orElseThrow(() -> new InvalidQueryJobException(queryJobId,
                                "The entry in query_job table corresponding to this search could not be located."));

                try {
                    if (queryJob.getSchema() != null) {
                        dataModel = new ObjectMapper().readValue(queryJob.getSchema(), DataModel.class);
                        log.info("Table schema (from tables-registry) retrieved successfully from 'query_job' postgres table.");
                        tableData.setDataModel(dataModel);
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException("Exception while reading table schema from 'query_job' table.", ex);
                }
            }
        }
        // There is no need to populate the dataModel if there is no tableData
        else {
            tableData.setDataModel(null);
        }
    }

    private void logAuditEvent(HttpServletRequest request, String action, String outcome, Map<String, Object> extraArguments) {
        final TraceContext context = tracer.currentSpan().context();
        auditEventLogger.log(
            AuditEventBody.builder()
                .action(new AuditedAction(action))
                .outcome(new AuditedOutcome(outcome))
                .context(
                    AuditedContext.builder()
                        .traceId(context.traceIdString())
                        .spanId(context.spanIdString())
                        .build()
                )
                .resource(new AuditedResource(callbackBaseUrl(request) + request.getPathInfo()))
                .extraArguments(extraArguments)
                .build()
        );
    }
}
