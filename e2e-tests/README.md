# Intro

This directory contains end-to-end tests for this application. These tests rely on an artifact stored in DNAstack's public github packages repo. At this point in time, retrieving a public github package still requires *any* github identity. Thus, in order to build this package, it is *required* that you have a [Github access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) with package read permissions [configured as a credential to our repository.](http://maven.apache.org/settings.html)

An example of such a configuration is provided below.


```xml
<repositories>
   <repository>
       <id>github-public</id>
       <name>Github DNAstack Maven Packages</name>
       <url>https://maven.pkg.github.com/DNAstack/dnastack-public-packages</url>
   </repository>
</repositories>
```

```xml
<servers>
   <server>
       <id>github-public</id>
       <username>$GITHUB_USERNAME</username>
       <password>$PERSONAL_ACCESS_TOKEN</password>
   </server>
</servers>
```

# Running against a local stack

The `Data Connect Trino E2E Tests - Local` IntelliJ run configuration (in `e2e-tests/.run`) runs the whole
suite against services on their usual local ports. It expects wallet, data-connect-trino, Trino,
collection-service and indexing-service to be up, and it sets only the variables whose defaults in
`BaseE2eTest` and `DataConnectE2eTest` do not already describe a local stack:

| Variable | Why it is set |
|---|---|
| `E2E_GLOBAL_METHOD_SECURITY_ENABLED`, `E2E_SCOPE_CHECKING_ENABLED` | Default to `false`, which skips the authorization tests. |
| `E2E_INDEXING_SERVICE_ENABLED` | Has no default; the indexing-service test is disabled without it. |
| `E2E_SHOW_SCHEMA_FOR_CATALOG_NAME`, `E2E_SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME` | Have no defaults, because the catalog to query differs per environment. `data_lake` is the local equivalent of the chart's `publisher`. |

Everything else — base URIs, wallet client and the publisher-data resource — comes from the defaults in
the test code. Overriding them in the run configuration is how they drift.
