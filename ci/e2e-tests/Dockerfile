# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM amazoncorretto:21-alpine@sha256:c64034bdceaff59b33bc366b9ab1b7fc5593d8155daf82cd978541b206555d1f AS build
ARG APP_NAME
ARG APP_VERSION
WORKDIR /build

# Before bringing in project sources, resolve maven dependencies.
# This allows Docker to reuse the cached image with all dependencies resolved.
ADD target/pom.xml ./
ADD target/.mvn/ ./.mvn/
ADD target/mvnw ./mvnw
# Allows us to pass settings.xml configured on local machine or CI server to access private Nexus repo
ADD target/.m2 /root/.m2

RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}

# Delete settings*.xml files that contain GitHub credentials
RUN rm /root/.m2/settings*.xml

# Now add sources, which will bust the cache.
ADD target/src ./src
RUN ./mvnw -B -o process-test-classes

##
## Stage 2: Package the runnable test image
##

# This stage uses a JRE rather than JDK because it doesn't have to compile any Java sources.
# It is assembling the runnable test container and packaging all its Maven dependencies
FROM amazoncorretto:21-alpine@sha256:c64034bdceaff59b33bc366b9ab1b7fc5593d8155daf82cd978541b206555d1f
ARG APP_NAME
ARG APP_VERSION

WORKDIR /

# Create a non-root user
RUN adduser -D -h /home/testuser testuser

USER root
RUN mkdir /target && chown testuser /target

# Copy required files from build stage into image
COPY --from=build --chown=testuser /build/target/ /home/testuser/target
COPY --from=build --chown=testuser /root/.m2/ /home/testuser/.m2/
COPY --chown=testuser target/pom.xml /home/testuser/pom.xml
COPY --chown=testuser target/.mvn /home/testuser/.mvn
COPY --chown=testuser target/mvnw /home/testuser

USER testuser

WORKDIR /home/testuser

ENV E2E_SCREENSHOT_DIR /target

ENTRYPOINT ["./mvnw", "-B", "-o", "-DreportsDirectory=/target/surefire-reports", "test"]
