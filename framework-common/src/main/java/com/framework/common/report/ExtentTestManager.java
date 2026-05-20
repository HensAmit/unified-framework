package com.framework.common.report;

import com.aventstack.extentreports.ExtentTest;

/**
 * Thread-local holder for the currently active {@link ExtentTest}.
 *
 * <p>Each scenario, when running in parallel, executes on its own thread.
 * Each thread needs its own "current Extent test node" reference so that
 * step logs and screenshots land under the right scenario in the report.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Hook (or TestNG listener) creates a node and calls {@link #set(ExtentTest)}.</li>
 *   <li>Steps within that scenario call {@link #get()} to log progress.</li>
 *   <li>Hook calls {@link #remove()} at scenario end to prevent thread reuse leaks.</li>
 * </ol>
 *
 * <p>Failing to call {@link #remove()} in long-lived thread pools can cause
 * the previous scenario's reference to bleed into a new scenario sharing the
 * same thread. Always call {@code remove} in the {@code @After} hook.
 */
public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private ExtentTestManager() {
        // utility — no instances
    }

    /**
     * Sets the current scenario's {@link ExtentTest} for this thread.
     */
    public static void set(ExtentTest test) {
        CURRENT.set(test);
    }

    /**
     * Returns the current scenario's {@link ExtentTest}, or {@code null}
     * if none has been set on this thread.
     */
    public static ExtentTest get() {
        return CURRENT.get();
    }

    /**
     * Clears the thread-local reference. Must be called at scenario end
     * to prevent leaks across thread reuse.
     */
    public static void remove() {
        CURRENT.remove();
    }
}
