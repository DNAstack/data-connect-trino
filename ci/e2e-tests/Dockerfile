# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

# This stage depends on a JDK image and uses Maven Wrapper to pull in dependencies and build the project
# from source.
FROM amazoncorretto:17-alpine AS build
ARG APP_NAME
ARG APP_VERSION
WORKDIR /build

# Downloading Datadog tracing jar. This is coupled with the 0.1.8 plugin version explicitly specified in the pom.xml
# We need to use an exact java agent version because the latest version will depend on newer dd-javac-plugin versions
# This has to be included in the dynamic dependencies due to a problem with the DataDog k8s admission controller that's
# preventing the dependencies from being injected automatically at runtime
RUN wget -O dd-tracer.jar https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.24.2/dd-java-agent-1.24.2.jar


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
FROM amazoncorretto:17-alpine
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
COPY --from=build /build/dd-tracer.jar /home/testuser/dd-tracer.jar
COPY --chown=testuser target/pom.xml /home/testuser/pom.xml
COPY --chown=testuser target/.mvn /home/testuser/.mvn
COPY --chown=testuser target/mvnw /home/testuser

USER testuser

WORKDIR /home/testuser

ENV MAVEN_OPTS "-javaagent:/home/testuser/dd-tracer.jar=dd.civisibility.enabled=true,dd.service=$APP_NAME"
ENV E2E_SCREENSHOT_DIR /target

ENTRYPOINT ["./mvnw", "-B", "-o", "-DreportsDirectory=/target/surefire-reports", "test"]