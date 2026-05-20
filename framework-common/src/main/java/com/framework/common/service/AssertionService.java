package com.framework.common.service;

import com.framework.common.context.TestContext;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft-assertion service used by step definitions and other framework code.
 *
 * <p>The "soft" part is the key behaviour: when an assertion fails it is
 * <em>recorded</em>, not thrown. The scenario continues and further assertions
 * still execute. At scenario end, {@link #assertAll()} throws a single
 * {@link AssertionError} that lists every failure that accumulated.
 *
 * <p>This matters for API tests where one response is checked against many
 * fields — fail-fast would tell you the status code is wrong but never get to
 * the body assertion that reveals <em>why</em>. With soft assertions, you get
 * the full picture of what's broken from one run.
 *
 * <p>Lifecycle (typical, wired up in Phase 3+):
 * <ol>
 *   <li>PicoContainer creates one {@code AssertionService} per scenario.</li>
 *   <li>Steps call {@link #assertEquals(String, Object, Object)} and friends.</li>
 *   <li>An {@code @After} hook calls {@link #assertAll()} to fail the scenario
 *       if anything was recorded.</li>
 *   <li>{@link #getFailureMessages()} can be called <em>before</em> {@code assertAll}
 *       to feed failure text into the Extent report.</li>
 * </ol>
 *
 * <p>For Phase 2 only primitive assertions are exposed. The richer DataTable-
 * driven methods ({@code status}, {@code jsonpath}, {@code schema}, etc.)
 * arrive in Phase 4 as thin wrappers over these primitives.
 */
public class AssertionService {

    private static final Logger log = LogUtils.getLogger(AssertionService.class);

    private final SoftAssertions soft;
    private final List<String> recordedFailures;
    @SuppressWarnings("unused")
    private final TestContext ctx;     // not consulted yet — wired for Phase 3+ usage

    /**
     * PicoContainer-injectable constructor.
     */
    public AssertionService(TestContext ctx) {
        this.ctx = ctx;
        this.soft = new SoftAssertions();
        this.recordedFailures = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Primitive assertions — building blocks for Phase 4's DataTable types
    // -------------------------------------------------------------------------

    public void assertEquals(String description, Object actual, Object expected) {
        log.debug("Asserting [{}]: expected={} actual={}", description, expected, actual);
        soft.assertThat(actual).as(description).isEqualTo(expected);
    }

    public void assertNotNull(String description, Object actual) {
        log.debug("Asserting [{}]: not null, actual={}", description, actual);
        soft.assertThat(actual).as(description).isNotNull();
    }

    public void assertNull(String description, Object actual) {
        log.debug("Asserting [{}]: is null, actual={}", description, actual);
        soft.assertThat(actual).as(description).isNull();
    }

    public void assertContains(String description, String actual, String expectedSubstring) {
        log.debug("Asserting [{}]: '{}' contains '{}'", description, actual, expectedSubstring);
        soft.assertThat(actual).as(description).contains(expectedSubstring);
    }

    public void assertTrue(String description, boolean condition) {
        log.debug("Asserting [{}]: expected true, actual={}", description, condition);
        soft.assertThat(condition).as(description).isTrue();
    }

    public void assertLessThanOrEqualTo(String description, long actual, long max) {
        log.debug("Asserting [{}]: {} <= {}", description, actual, max);
        soft.assertThat(actual).as(description).isLessThanOrEqualTo(max);
    }

    // -------------------------------------------------------------------------
    // Inspection — used by Extent reporting before assertAll() is called
    // -------------------------------------------------------------------------

    /**
     * Returns the list of assertion failure messages recorded so far.
     * Safe to call before {@link #assertAll()}; does not throw.
     *
     * <p>Used by the {@code @After} hook to attach failure detail to the
     * Extent report node before the scenario fails.
     */
    public List<String> getFailureMessages() {
        // SoftAssertions exposes errorsCollected() returning Throwable list
        List<String> messages = new ArrayList<>(recordedFailures);
        soft.errorsCollected().forEach(t -> messages.add(t.getMessage()));
        return messages;
    }

    /**
     * Returns true if any assertions have failed so far.
     */
    public boolean hasFailures() {
        return !soft.errorsCollected().isEmpty() || !recordedFailures.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Terminal — call at scenario end
    // -------------------------------------------------------------------------

    /**
     * Throws if any recorded assertions failed. Call exactly once per scenario,
     * from an {@code @After} hook.
     *
     * <p>If no assertions failed, returns silently.
     *
     * @throws AssertionError describing every accumulated failure
     */
    public void assertAll() {
        if (!recordedFailures.isEmpty()) {
            // We accumulated extra failures outside SoftAssertions; surface them
            // by adding them to soft before delegating.
            recordedFailures.forEach(msg -> soft.fail(msg));
        }
        soft.assertAll();
    }

    /**
     * Records a failure message directly, without an associated AssertJ
     * comparison. Useful for "this step shouldn't have been reached" cases.
     */
    public void fail(String message) {
        log.debug("Recording manual failure: {}", message);
        recordedFailures.add(message);
    }
}
