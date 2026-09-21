# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Extract the tenant-lifecycle changelog, which ships inside the library jar
##

FROM amazoncorretto:21-alpine@sha256:fda60fd7965970ce7ed7ce789b18418647b56ac6112fc17df006337bdc6355c4 AS build
WORKDIR /build

ADD target/pom.xml ./
ADD target/.mvn/ ./.mvn/
ADD target/mvnw ./mvnw
# Allows us to pass settings.xml configured on local machine or CI server to access private Maven repositories
ADD target/.m2 /root/.m2

# help:evaluate writes the version to a file rather than to stdout, so that a failed
# evaluation fails this layer instead of being substituted into the coordinate below.
RUN ./mvnw -B help:evaluate -Dexpression=spring-boot-tenancy.version -Doutput=tenancy.version
RUN ./mvnw -B dependency:unpack \
      -Dartifact=com.dnastack:spring-boot-tenancy-lifecycle:$(cat tenancy.version) \
      -Dincludes='db/changelog/db.changelog-tenant-lifecycle-setup.yaml' \
      -DoutputDirectory=tenancy

# Delete settings*.xml files that contain GitHub credentials
RUN rm /root/.m2/settings*.xml

##
## Stage 2: Package the changelogs for liquibase to run
##

FROM gcr.io/dnastack-container-store/liquibase-docker-image:1.0-2-gf48af98@sha256:ce4a85cc6cb6d3a6914cac6d1abb4eff2b718435c8565930c05fae564379be25
ADD target/db.changelog-master.yaml /db.changelog-master.yaml
COPY --from=build /build/tenancy/db/changelog/db.changelog-tenant-lifecycle-setup.yaml /db.changelog-tenant-lifecycle-setup.yaml
