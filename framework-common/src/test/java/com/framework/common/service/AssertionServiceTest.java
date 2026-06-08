package com.framework.common.service;

import com.framework.common.context.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssertionServiceTest {

    private TestContext ctx;
    private AssertionService assertions;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        assertions = new AssertionService(ctx);
    }

    // --- passing assertions do not throw ---

    @Test
    @DisplayName("assertEquals does not throw when values match")
    void assertEqualsPasses() {
        assertDoesNotThrow(() -> assertions.assertEquals("equal", 200, 200));
        assertNull(ctx.getFailureStackTrace(), "no trace should be captured on success");
    }

    @Test
    @DisplayName("assertNotNull does not throw for a non-null value")
    void assertNotNullPasses() {
        assertDoesNotThrow(() -> assertions.assertNotNull("present", "x"));
    }

    @Test
    @DisplayName("assertContains does not throw when substring is present")
    void assertContainsPasses() {
        assertDoesNotThrow(() -> assertions.assertContains("contains", "Radiohead", "Radio"));
    }

    @Test
    @DisplayName("assertLessThanOrEqualTo does not throw when within bound")
    void assertLessThanOrEqualToPasses() {
        assertDoesNotThrow(() -> assertions.assertLessThanOrEqualTo("time", 100L, 2000L));
    }

    // --- failing assertions throw immediately (hard) ---

    @Test
    @DisplayName("assertEquals throws immediately when values differ")
    void assertEqualsFailsHard() {
        assertThrows(AssertionError.class,
                () -> assertions.assertEquals("status", 200, 201));
    }

    @Test
    @DisplayName("assertNotNull throws for null")
    void assertNotNullFailsHard() {
        assertThrows(AssertionError.class,
                () -> assertions.assertNotNull("present", null));
    }

    @Test
    @DisplayName("assertTrue throws for false")
    void assertTrueFailsHard() {
        assertThrows(AssertionError.class,
                () -> assertions.assertTrue("flag", false));
    }

    @Test
    @DisplayName("fail throws immediately")
    void failThrowsHard() {
        assertThrows(AssertionError.class,
                () -> assertions.fail("boom"));
    }

    // --- a failure captures the stack trace into the context ---

    @Test
    @DisplayName("a failed assertion captures its stack trace into the context")
    void capturesStackTraceOnFailure() {
        assertThrows(AssertionError.class,
                () -> assertions.assertEquals("status", 200, 201));
        String trace = ctx.getFailureStackTrace();
        assertNotNull(trace, "stack trace should be captured");
        assertFalse(trace.isBlank(), "stack trace should not be blank");
        // A real stack trace has frame lines: newline + tab + "at ".
        assertTrue(trace.contains("\n\tat "), "trace should contain stack frames");
    }

    @Test
    @DisplayName("failWith captures the given error's trace and rethrows it")
    void failWithRethrows() {
        AssertionError original = new AssertionError("schema mismatch");
        AssertionError thrown = assertThrows(AssertionError.class,
                () -> assertions.failWith(original));
        assertSame(original, thrown, "the same error instance should propagate");
        assertNotNull(ctx.getFailureStackTrace());
    }
}
