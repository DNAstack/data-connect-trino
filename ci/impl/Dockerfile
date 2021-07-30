# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

FROM openjdk:11-jdk-slim AS build
ARG APP_NAME
ARG APP_VERSION
WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.

ADD target/pom.xml /build/pom.xml
ADD target/src /build/src
ADD target/.mvn/ /build/.mvn/
ADD target/mvnw /build/mvnw
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2

RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}
RUN ./mvnw -B -X package

##
## Stage 2: Package the runnable image
##

# This stage uses a JRE rather than JDK because it doesn't have to build anything; it just runs the fat jar
# produced by the previous stage.
FROM openjdk:11-slim
ARG APP_NAME
ARG APP_VERSION

COPY --from=0 /build/target/ga4gh-discovery-search-service-*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar", "app.jar", "-XX:MaxRAMPercentage=90"]
