package com.framework.ui.driver;

import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Holds the active {@link WebDriver} for the current thread.
 *
 * <p>Phase 7 runs scenarios in parallel — each on its own thread, each driving
 * its own browser. A {@link ThreadLocal} gives every thread its own driver
 * reference with no sharing, so parallel scenarios can't trip over each other's
 * browser. This mirrors how {@code ExtentTestManager} isolates the report node
 * per thread.
 *
 * <p>The driver lives here rather than in {@code TestContext} so that page
 * objects depend only on "a driver is available," not on the whole scenario
 * context. {@link DriverFactory} builds the driver; the
 * UI {@code @Before} hook stores it here; page objects read it.
 */
public final class DriverManager {

    private static final Logger log = LogUtils.getLogger(DriverManager.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // utility — no instances
    }

    /** Stores the driver for the current thread. Called by the UI @Before hook. */
    public static void setDriver(WebDriver driver) {
        DRIVER.set(driver);
    }

    /**
     * Returns the current thread's driver.
     *
     * @throws IllegalStateException if no driver is set — usually means the UI
     *         {@code @Before} hook didn't run (e.g. a UI step ran in a non-@ui scenario).
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver for this thread. Was the UI @Before hook executed "
                            + "(is the scenario tagged @ui)?");
        }
        return driver;
    }

    /** Quits the driver and clears the thread-local. Safe to call when none is set. */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Error quitting driver: {}", e.getMessage());
            } finally {
                DRIVER.remove();
            }
        }
    }
}
