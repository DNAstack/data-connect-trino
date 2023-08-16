# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM azul/zulu-openjdk:17.0.3 AS build
ARG APP_NAME
ARG APP_VERSION
WORKDIR /build

# Downloading Datadog tracing jar
RUN wget -O dd-tracer.jar https://dtdg.co/latest-java-tracer

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn/ ./.mvn/
ADD target/mvnw ./mvnw
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2

RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}

# Now add sources, which will bust the cache.
ADD target/src ./src
RUN ./mvnw -B -o package -DskipTests=true

##
## Stage 2: Package the runnable test image
##

# This stage uses a JRE rather than JDK because it doesn't have to compile any Java sources.
# It is assembling the runnable test container and packaging all its Maven dependencies
FROM azul/zulu-openjdk:17.0.3-jre
ARG APP_NAME
ARG APP_VERSION

WORKDIR /
ADD target/e2e-exec-pom.xml /pom.xml
ADD target/.mvn/ /.mvn/
ADD target/mvnw /

COPY --from=0 /build/target/data-connect-trino-e2e-tests-*.jar /e2e-tests.jar
COPY --from=0 /build/dd-tracer.jar /dd-tracer.jar

ENV MAVEN_OPTS "-javaagent:/dd-tracer.jar=dd.civisibility.enabled=true,dd.service=$APP_NAME"
RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

ENTRYPOINT ["./mvnw", "-o", "test"]
