#!/usr/bin/env bash
# Waits for the Selenium Grid to report ready, then runs the test suite with the
# arguments passed to the container (CMD or a compose `command:` override).
#
# depends_on only guarantees the Grid containers have *started*, not that the
# nodes have *registered* with the hub. So we poll the Grid status endpoint
# until it reports ready before launching the tests — otherwise the first
# session request can fail with "no slot matched".
set -euo pipefail

GRID_STATUS_URL="${GRID_STATUS_URL:-http://selenium-hub:4444/status}"
MAX_ATTEMPTS="${GRID_WAIT_ATTEMPTS:-30}"
SLEEP_SECONDS="${GRID_WAIT_SLEEP:-2}"

echo "Waiting for Selenium Grid: ${GRID_STATUS_URL}"
for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
    if curl -sSf "${GRID_STATUS_URL}" 2>/dev/null | grep -qE '"ready"\s*:\s*true'; then
        echo "Grid is ready."
        exec mvn -B test -pl framework-tests -am "$@"
    fi
    echo "  Grid not ready yet (${attempt}/${MAX_ATTEMPTS}); retrying in ${SLEEP_SECONDS}s..."
    sleep "${SLEEP_SECONDS}"
done

echo "ERROR: Selenium Grid did not become ready after ${MAX_ATTEMPTS} attempts." >&2
exit 1
