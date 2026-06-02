package com.framework.api.utils;

import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Polling helper for async API workflows, built on Awaitility.
 *
 * <p>Some APIs return "202 Accepted, job queued" and you must poll a status
 * endpoint until the job completes. {@link #waitUntil} repeatedly invokes a
 * supplier until a condition holds or a timeout elapses.
 *
 * <p>Example:
 * <pre>{@code
 * Response done = WaitUtils.waitUntil(
 *     () -> apiService.get("/jobs/" + jobId),
 *     resp -> "COMPLETED".equals(((Response) resp).jsonPath().getString("status")),
 *     Duration.ofSeconds(30),
 *     Duration.ofSeconds(2));
 * }</pre>
 *
 * <p>Spotify's read endpoints are synchronous, so there's no live demo of this
 * in Phase 4 — but the utility is part of the framework's capability and any
 * async API (or future module) can use it.
 */
public final class WaitUtils {

    private static final Logger log = LogUtils.getLogger(WaitUtils.class);

    private WaitUtils() {
        // utility — no instances
    }

    /**
     * Polls {@code supplier} every {@code pollInterval} until {@code condition}
     * returns true or {@code timeout} elapses.
     *
     * @return the last value produced by {@code supplier} that satisfied the condition
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout elapses first
     */
    public static <T> T waitUntil(Supplier<T> supplier,
                                  Predicate<T> condition,
                                  Duration timeout,
                                  Duration pollInterval) {
        log.info("Polling for condition (timeout={}, interval={})", timeout, pollInterval);
        AtomicReference<T> lastValue = new AtomicReference<>();
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(pollInterval)
                .until(() -> {
                    T value = supplier.get();
                    lastValue.set(value);
                    return condition.test(value);
                });
        return lastValue.get();
    }
}
