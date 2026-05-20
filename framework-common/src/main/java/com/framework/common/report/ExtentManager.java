package com.framework.common.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton lifecycle for the Extent HTML report.
 *
 * <p>{@link #get()} returns the {@link ExtentReports} instance, lazily created
 * on first access (initialisation-on-demand holder pattern, same as
 * {@link ConfigManager}).
 *
 * <p>{@link #flush()} writes the report to disk and is called once at suite
 * teardown by the TestNG listener (introduced in Phase 7). The framework
 * never closes the report mid-suite.
 *
 * <p>Output path: {@code target/reports/extent-report.html} (single
 * self-contained HTML file with embedded screenshots).
 */
public final class ExtentManager {

    private static final Logger log = LogUtils.getLogger(ExtentManager.class);

    /** Default report path; created if absent. */
    private static final Path REPORT_DIR = Paths.get("target", "reports");
    private static final String REPORT_FILE = "extent-report.html";

    private ExtentManager() {
        // utility — no instances
    }

    /**
     * Returns the singleton {@link ExtentReports} instance.
     */
    public static ExtentReports get() {
        return Holder.INSTANCE;
    }

    /**
     * Persists the report to disk. Safe to call multiple times.
     * Should be invoked once at suite end.
     */
    public static void flush() {
        Holder.INSTANCE.flush();
        log.info("Extent report flushed to {}/{}", REPORT_DIR, REPORT_FILE);
    }

    private static final class Holder {
        private static final ExtentReports INSTANCE = build();

        private static ExtentReports build() {
            try {
                Files.createDirectories(REPORT_DIR);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create report directory: " + REPORT_DIR, e);
            }

            Path reportPath = REPORT_DIR.resolve(REPORT_FILE);
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toString());
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Unified Framework Report");
            spark.config().setReportName("UI + API Automation");
            spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
            spark.config().setEncoding("UTF-8");

            ExtentReports extent = new ExtentReports();
            extent.attachReporter(spark);

            // Surface useful context in the report header — interview-grade detail
            extent.setSystemInfo("Environment", ConfigManager.get().env());
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("User", System.getProperty("user.name"));

            log.info("Extent report initialised at {}", reportPath.toAbsolutePath());
            return extent;
        }
    }
}
