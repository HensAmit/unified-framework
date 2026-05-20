package com.framework.common.service;

import com.framework.common.context.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssertionServiceTest {

    private AssertionService assertions;

    @BeforeEach
    void setUp() {
        assertions = new AssertionService(new TestContext());
    }

    @Test
    @DisplayName("assertAll succeeds silently when no failures recorded")
    void noFailuresPassesCleanly() {
        assertions.assertEquals("equality", "x", "x");
        assertions.assertNotNull("non-null", "y");
        assertions.assertTrue("boolean true", true);

        assertDoesNotThrow(() -> assertions.assertAll());
        assertFalse(assertions.hasFailures());
        assertTrue(assertions.getFailureMessages().isEmpty());
    }

    @Test
    @DisplayName("hasFailures becomes true after any failed assertion")
    void hasFailuresAfterFailedAssertion() {
        assertFalse(assertions.hasFailures());

        assertions.assertEquals("intentional fail", "actual", "expected");

        assertTrue(assertions.hasFailures());
    }

    @Test
    @DisplayName("getFailureMessages collects all failure descriptions")
    void collectsAllFailures() {
        assertions.assertEquals("first", 1, 2);
        assertions.assertEquals("second", "a", "b");
        assertions.assertNotNull("third", null);

        List<String> messages = assertions.getFailureMessages();
        assertEquals(3, messages.size());
        // Each description should appear in exactly one message
        assertTrue(messages.stream().anyMatch(m -> m.contains("first")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("second")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("third")));
    }

    @Test
    @DisplayName("assertAll throws AssertionError listing every failure")
    void assertAllThrowsWithAllFailures() {
        assertions.assertEquals("first", 1, 2);
        assertions.assertEquals("second", "a", "b");

        AssertionError err = assertThrows(AssertionError.class, () -> assertions.assertAll());
        // AssertJ's MultipleFailuresError includes both descriptions
        assertTrue(err.getMessage().contains("first"));
        assertTrue(err.getMessage().contains("second"));
    }

    @Test
    @DisplayName("scenario continues collecting after a failure (soft semantics)")
    void softSemantics() {
        assertions.assertEquals("first will fail", 1, 2);
        // The fact that we reach this line at all proves soft semantics
        assertions.assertEquals("second still runs", "x", "x");

        // Only the first one failed
        assertEquals(1, assertions.getFailureMessages().size());
    }

    @Test
    @DisplayName("manual fail() records a message")
    void manualFail() {
        assertions.fail("unexpected branch reached");
        assertTrue(assertions.hasFailures());
        AssertionError err = assertThrows(AssertionError.class, () -> assertions.assertAll());
        assertTrue(err.getMessage().contains("unexpected branch"));
    }

    @Test
    @DisplayName("assertContains works as substring check")
    void contains() {
        assertions.assertContains("substr present",  "hello world", "world");
        assertions.assertContains("substr missing",  "hello world", "xyz");
        assertEquals(1, assertions.getFailureMessages().size());
    }

    @Test
    @DisplayName("assertLessThanOrEqualTo enforces upper bound")
    void lessThanOrEqual() {
        assertions.assertLessThanOrEqualTo("under bound", 100, 200);
        assertions.assertLessThanOrEqualTo("at bound", 200, 200);
        assertions.assertLessThanOrEqualTo("over bound", 300, 200);
        assertEquals(1, assertions.getFailureMessages().size());
    }
}
