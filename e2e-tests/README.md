# Intro

This directory contains end-to-end tests for this application. They depend on artifacts in DNAstack's
GitHub Packages repository, which `e2e-tests/pom.xml` declares as the `github` repository. GitHub
Packages requires an authenticated identity to read a Maven package, so building this module needs a
[GitHub access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
with `read:packages` [configured as a credential](http://maven.apache.org/settings.html) under the
matching id in your `~/.m2/settings.xml`.

```xml
<servers>
   <server>
       <id>github</id>
       <username>$GITHUB_USERNAME</username>
       <password>$PERSONAL_ACCESS_TOKEN</password>
   </server>
</servers>
```

The root project needs the same credential for its own dependencies; see
[Maven Repository Credentials](../README.md#maven-repository-credentials).

# Running against a local stack

The `Data Connect Trino E2E Tests - Local` IntelliJ run configuration (in `e2e-tests/.run`) runs the whole
suite against services on their usual local ports. It expects wallet, data-connect-trino, Trino,
collection-service and indexing-service to be up, and it sets only the variables whose defaults in
`BaseE2eTest` and `DataConnectE2eTest` do not already describe a local stack:

- `E2E_GLOBAL_METHOD_SECURITY_ENABLED` and `E2E_SCOPE_CHECKING_ENABLED` default to `false`, which skips the
  authorization tests.
- `E2E_INDEXING_SERVICE_ENABLED` has no default, and the indexing-service test is disabled without it.
- `E2E_SHOW_SCHEMA_FOR_CATALOG_NAME` and `E2E_SHOW_TABLE_FOR_CATALOG_SCHEMA_NAME` have no defaults, because the
  catalog to query differs per environment. `data_lake` is the local equivalent of the chart's `publisher`.

Everything else — base URIs, wallet client and the publisher-data resource — comes from the defaults in
the test code. Overriding them in the run configuration is how they drift.

# The catalog the tests work in

Each run creates a catalog of its own, `dnastack_e2etest_data_connect_trino_<epoch millis>`, and drops it
when it finishes. Trino drops a catalog's schemas and tables along with it, so a test that makes a table
needs no cleanup of its own.

Two things follow from the catalog being the run's own. It has to be created, which is why the suite asks
for `data-connect:manage` on publisher-data and why the chart grants it. And nobody registers a connector
for a catalog named after the moment it was created, so no indexer sees these tables — which matters
because an indexed table would compete with the library entry
`getTableInfo_should_returnCustomSchema_from_indexingService` registers for a table of its own.

A run that is killed leaves its catalog behind. The timestamp in the name is what lets the next run
recognise one that no run can still be using and drop it.
