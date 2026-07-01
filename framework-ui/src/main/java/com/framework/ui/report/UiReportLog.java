package com.framework.ui.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.utils.LogUtils;
import com.framework.ui.driver.DriverManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * UI-specific report logging that attaches a screenshot to the current
 * scenario's Extent node. Reads the per-thread WebDriver (DriverManager) and
 * ExtentTest (ExtentTestManager), so it is parallel-safe: each scenario thread
 * logs to its own node.
 *
 * <p>The screenshot is embedded as an inline base64 image at a readable width
 * (shown by default, no click required) rather than via MediaEntityBuilder,
 * which renders a click-to-open link. The image is capped at 900px wide and
 * scales down on narrow reports. It stays embedded in the HTML, so the report
 * remains a single self-contained file.
 *
 * <p>No-ops safely if no Extent node is set (falls back to Log4j) and tolerates
 * a missing driver or capture failure (logs the text without an image), so a
 * screenshot problem never breaks the test flow.
 */
public final class UiReportLog {

    private static final Logger log = LogUtils.getLogger(UiReportLog.class);

    private UiReportLog() {}

    public static void infoWithScreenshot(String message) {
        logWithScreenshot(Status.INFO, message);
    }

    public static void passWithScreenshot(String message) {
        logWithScreenshot(Status.PASS, message);
    }

    public static void info(String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test != null) {
            test.log(Status.INFO, message);
        }
        log.info(message);
    }

    public static void pass(String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test != null) {
            test.log(Status.PASS, message);
        }
        log.info(message);
    }

    private static void logWithScreenshot(Status status, String message) {
        ExtentTest test = ExtentTestManager.get();
        if (test == null) {
            log.info("[no Extent node] {}", message);
            return;
        }

        String base64 = captureBase64();
        if (base64 != null) {
            String dataUri = "data:image/png;base64," + base64;
            String html = message + "<br>"
                    + "<img src='" + dataUri + "' "
                    + "style='width:100%; max-width:900px; border:1px solid #ccc; border-radius:4px;'/>";
            test.log(status, html);
        } else {
            // Driver unavailable or capture failed — still log the text.
            test.log(status, message);
        }
        log.info(message);
    }

    /** Returns a base64 PNG of the current screen, or null if unavailable. */
    private static String captureBase64() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            return null;
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            // Never let a screenshot failure break the test flow.
            log.warn("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }
}