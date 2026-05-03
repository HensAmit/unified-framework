package com.framework.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lightweight convenience wrapper around Log4j2.
 *
 * <p>Most framework code should declare its own logger directly:
 * <pre>{@code
 * private static final Logger log = LogManager.getLogger(MyClass.class);
 * }</pre>
 *
 * <p>This class exists for two cases:
 * <ul>
 *   <li>{@link #getLogger(Class)} — when a one-line getter is preferable to the
 *       full {@code LogManager.getLogger} import.</li>
 *   <li>{@link #banner(String)} — for framework lifecycle events (suite start,
 *       config load, etc.) where a uniform visual marker helps when scanning logs.</li>
 * </ul>
 */
public final class LogUtils {

    private static final Logger FRAMEWORK_LOG = LogManager.getLogger("FRAMEWORK");
    private static final String LINE = "=".repeat(70);

    private LogUtils() {
        // utility class — no instances
    }

    /**
     * Returns a Log4j2 logger for the given class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    /**
     * Logs a visually distinct banner at INFO level under the {@code FRAMEWORK} logger.
     * Useful for marking suite start, config load, environment switches, etc.
     */
    public static void banner(String message) {
        FRAMEWORK_LOG.info(LINE);
        FRAMEWORK_LOG.info(message);
        FRAMEWORK_LOG.info(LINE);
    }
}
