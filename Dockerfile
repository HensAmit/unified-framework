# =============================================================================
# Test-runner image: JDK 17 + Maven + the framework.
#
# This image does NOT contain a browser. It runs the Java/Maven test suite and
# drives browsers remotely on a Selenium Grid over the network. The browsers
# live in the Grid node containers (see docker-compose.yml).
# =============================================================================
FROM maven:3.9-eclipse-temurin-21

WORKDIR /workspace

# curl is used by the entrypoint to wait for the Grid to become ready.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Entrypoint copied first so editing source doesn't invalidate this layer.
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Copy the project and build it (skipping tests). This resolves every
# dependency and installs the internal modules into the image's local .m2, so
# the test run at container start is fast and needs no network for Maven.
COPY . .
RUN mvn -B -q install -DskipTests

# The entrypoint waits for the Grid, then runs `mvn test` with whatever args CMD
# (or a compose `command:` override) supplies.
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

# Default run: the UI suite against the Grid, headless. Override via compose.
CMD ["-Dcucumber.filter.tags=@ui", \
     "-Dui.grid.enabled=true", \
     "-Dui.grid.url=http://selenium-hub:4444/wd/hub", \
     "-Dui.headless=true"]
