# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

FROM amazoncorretto:21-alpine AS build
ARG APP_NAME
ARG APP_VERSION

RUN adduser -D -h /build build && chown build:build /build
USER build
WORKDIR /build

# Add the memory calculator to the image
RUN wget -O - https://storage.googleapis.com/dnastack-public-build-artifacts/memory-calculator-4.1.0.tgz \
    | tar -zxv

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
FROM amazoncorretto:21-alpine
ARG APP_NAME
ARG APP_VERSION

# gcompat required for statically-linked java-buildpack-memory-calculator to work under alpine
RUN apk add --no-cache gcompat

RUN busybox adduser -D -h / ${APP_NAME}
USER ${APP_NAME}

COPY --from=0 /build/target/data-connect-trino-*.jar /app.jar
COPY --from=0 /build/java-buildpack-memory-calculator /java-buildpack-memory-calculator

EXPOSE 8080
#ENTRYPOINT exec java ${JAVA_OPTS} -jar app.jar
# If the container has a memory limit, that limit (in bytes) is present in a file under /sys/fs/cgroup
# In Linux 5.10.76, the value comes from memory.max.
# If the container doesn't have a set memory limit, the file contains "max", hence we fall back to a default value of 2GiB
# In Linux 5.4.129, the value comes from memory/memory.limit_in_bytes
# If the container doesn't have a set memory limit, the file will be blank, hence we fall back to a default value of 2GiB
CMD container_mem=$(cat "/sys/fs/cgroup/memory.max" || cat "/sys/fs/cgroup/memory/memory.limit_in_bytes") ; \
    exec java $(/java-buildpack-memory-calculator \
    --total-memory=$([ "$container_mem" = "max" -o -z "$container_mem" -o "$container_mem" = "9223372036854771712" ] && echo "2097152000" || echo "$container_mem")B \
    --loaded-class-count="${ESTIMATED_LOADED_CLASSES:-20000}" \
    --thread-count="${ESTIMATED_THREADS:-50}" \
    --head-room="${HEAD_ROOM:-10}" \
    --jvm-options="${JAVA_OPTS}") ${JAVA_OPTS} \
    -jar app.jar
