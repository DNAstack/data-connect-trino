# This is a two stage build, as described in the Docker docs: https://docs.docker.com/develop/develop-images/multistage-build/

##
## Stage 1: Build and test the Java Sources
##

FROM amazoncorretto:21-alpine@sha256:c64034bdceaff59b33bc366b9ab1b7fc5593d8155daf82cd978541b206555d1f AS build
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

RUN ./mvnw -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
RUN ./mvnw -B versions:set -DnewVersion=${APP_VERSION}
RUN ./mvnw -B -X package

##
## Stage 2: Package the runnable image
##

# This stage uses a JRE rather than JDK because it doesn't have to build anything; it just runs the fat jar
# produced by the previous stage.
FROM amazoncorretto:21-alpine@sha256:c64034bdceaff59b33bc366b9ab1b7fc5593d8155daf82cd978541b206555d1f
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
ENV MAX_MEMORY_UNLIMITED_VALUE="9223372036854771712"
ENV DEFAULT_MEMORY="2097152000"
ENV DIRECT_MEMORY_PERCENT="1"
ENV BYTES_TO_MB_FACTOR="1048576"
ENV MIN_DIRECT_MEMORY_MB="100"
ENV DEFAULT_LOADED_CLASSES="20000"
ENV DEFAULT_THREADS="50"
ENV DEFAULT_HEAD_ROOM="10"
CMD container_mem=$(cat "/sys/fs/cgroup/memory.max" || cat "/sys/fs/cgroup/memory/memory.limit_in_bytes") ; \
    effective_mem=$([ "$container_mem" = "max" -o -z "$container_mem" -o "$container_mem" = "$MAX_MEMORY_UNLIMITED_VALUE" ] && echo "$DEFAULT_MEMORY" || echo "$container_mem"); \
    direct_mem_mb=$((effective_mem * DIRECT_MEMORY_PERCENT / 100 / BYTES_TO_MB_FACTOR)); \
    direct_mem_mb=$((direct_mem_mb < MIN_DIRECT_MEMORY_MB ? MIN_DIRECT_MEMORY_MB : direct_mem_mb)); \
    echo "Calculated direct memory: ${direct_mem_mb}MB from ${effective_mem} bytes total memory (${DIRECT_MEMORY_PERCENT}%, min ${MIN_DIRECT_MEMORY_MB}MB)"; \
    direct_mem_option="-XX:MaxDirectMemorySize=${direct_mem_mb}M"; \
    exec java $(/java-buildpack-memory-calculator \
    --total-memory=${effective_mem}B \
    --loaded-class-count="${ESTIMATED_LOADED_CLASSES:-$DEFAULT_LOADED_CLASSES}" \
    --thread-count="${ESTIMATED_THREADS:-$DEFAULT_THREADS}" \
    --head-room="${HEAD_ROOM:-$DEFAULT_HEAD_ROOM}" \
    --jvm-options="${JAVA_OPTS} ${direct_mem_option}") ${JAVA_OPTS} \
    -jar app.jar
