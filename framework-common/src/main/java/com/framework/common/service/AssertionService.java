package com.framework.common.service;

import com.framework.common.context.TestContext;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Hard-assertion service. Each assertion fails <strong>immediately</strong> —
 * the first failure throws an {@link AssertionError}, which Cucumber catches and
 * marks the scenario failed, aborting the remaining steps.
 *
 * <p>Before rethrowing, the failing assertion's full stack trace is captured
 * into {@link TestContext#setFailureStackTrace(String)} so the reporting hook
 * can show in the Extent report the same trace seen on the console.
 *
 * <p>Lives in {@code framework-common} so both API and UI assertions share this
 * behaviour. Wraps AssertJ's (hard) {@link Assertions} for rich failure
 * messages.
 *
 * <p>Note: because assertions fail fast, a DataTable with several assertion rows
 * stops at the first failing row — you see one failure per run, not all of them.
 * This is the deliberate trade-off of hard over soft assertions.
 */
public class AssertionService {

    private static final Logger log = LogUtils.getLogger(AssertionService.class);

    private final TestContext ctx;

    public AssertionService(TestContext ctx) {
        this.ctx = ctx;
    }

    public void assertEquals(String description, Object actual, Object expected) {
        check(description, () -> Assertions.assertThat(actual).as(description).isEqualTo(expected));
    }

    public void assertNotNull(String description, Object actual) {
        check(description, () -> Assertions.assertThat(actual).as(description).isNotNull());
    }

    public void assertNull(String description, Object actual) {
        check(description, () -> Assertions.assertThat(actual).as(description).isNull());
    }

    public void assertContains(String description, String actual, String expectedSubstring) {
        check(description, () -> Assertions.assertThat(actual).as(description).contains(expectedSubstring));
    }

    public void assertTrue(String description, boolean condition) {
        check(description, () -> Assertions.assertThat(condition).as(description).isTrue());
    }

    public void assertLessThanOrEqualTo(String description, long actual, long max) {
        check(description, () -> Assertions.assertThat(actual).as(description).isLessThanOrEqualTo(max));
    }

    /** Fails immediately with a plain message (no value comparison). */
    public void fail(String message) {
        failWith(new AssertionError(message));
    }

    /**
     * Captures the trace of an externally-produced {@link AssertionError} (e.g.
     * a JSON Schema validation failure) and rethrows it, so such failures show
     * their trace in the report just like the wrapped assertions do.
     */
    public void failWith(AssertionError error) {
        captureTrace(error);
        log.error("Assertion FAILED: {}", error.getMessage());
        throw error;
    }

    /**
     * Runs an AssertJ assertion; on failure, captures the trace, logs it, and
     * rethrows so the scenario fails hard.
     */
    private void check(String description, Runnable assertion) {
        log.debug("Asserting [{}]", description);
        try {
            assertion.run();
        } catch (AssertionError e) {
            captureTrace(e);
            log.error("Assertion FAILED [{}]: {}", description, e.getMessage());
            throw e;
        }
    }

    private void captureTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        ctx.setFailureStackTrace(sw.toString());
    }
}
