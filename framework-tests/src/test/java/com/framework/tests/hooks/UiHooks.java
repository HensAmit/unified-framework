package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.common.context.TestContext;
import com.framework.common.report.ExtentManager;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.utils.LogUtils;
import com.framework.common.service.DbService;
import com.framework.ui.driver.DriverFactory;
import com.framework.ui.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Cucumber hooks for UI scenarios. Scoped to {@code @ui} so they never fire for
 * API scenarios (and {@code ApiHooks}, scoped to {@code @api}, never fires here).
 *
 * <p>{@code @Before}: create the Extent node and launch a browser via
 * {@link DriverFactory}, storing it in {@link DriverManager} for the thread.
 *
 * <p>{@code @After}: on failure, attach the stack trace and a screenshot
 * (embedded as an inline base64 thumbnail) — captured BEFORE quitting the
 * browser. The driver is always quit afterwards to avoid leaking browser
 * processes.
 *
 * <p>The Extent report is flushed once per suite by {@code ExtentFlushListener},
 * not here — under parallel execution a per-scenario flush would race across
 * threads. This hook only releases the thread-local test node and the browser.
 */
public class UiHooks {

    private static final Logger log = LogUtils.getLogger(UiHooks.class);

    private final TestContext ctx;
    private final DbService db;

    public UiHooks(TestContext ctx, DbService db) {
        this.ctx = ctx;
        this.db = db;
    }

    @Before(value = "@ui", order = 0)
    public void beforeScenario(Scenario scenario) {
        ctx.setScenarioName(scenario.getName());
        ctx.getScenarioTags().addAll(scenario.getSourceTagNames());

        ExtentTest test = ExtentManager.get().createTest(scenario.getName());
        scenario.getSourceTagNames().forEach(test::assignCategory);
        ExtentTestManager.set(test);

        log.info("Scenario START: {} {}", scenario.getName(), scenario.getSourceTagNames());

        WebDriver driver = DriverFactory.createDriver();
        DriverManager.setDriver(driver);
        db.connect();
    }

    @After("@ui")
    public void afterScenario(Scenario scenario) {
        ExtentTest test = ExtentTestManager.get();

        if (scenario.isFailed()) {
            test.log(Status.FAIL, "Scenario failed: " + scenario.getName()
                    + "<br>Location: " + scenario.getUri() + ":" + scenario.getLine());
            logStackTrace(test);
            attachScreenshot(test);   // must run before the driver is quit
        } else {
            test.log(Status.PASS, "Scenario passed: " + scenario.getName());
        }

        log.info("Scenario END:   {} -> {}", scenario.getName(), scenario.getStatus());

        // Report is flushed once per suite by ExtentFlushListener (parallel-safe).
        DriverManager.quitDriver();
        ExtentTestManager.remove();
        db.close();
    }

    private void logStackTrace(ExtentTest test) {
        String trace = ctx.getFailureStackTrace();
        if (trace == null || trace.isBlank()) {
            return;
        }
        test.log(Status.FAIL, "<b>Stack trace:</b><pre>" + escape(trace) + "</pre>");
    }

    /**
     * Captures the current browser screenshot and embeds it as an inline base64
     * image at a readable width — shown by default (no click required), unlike
     * MediaEntityBuilder which renders a click-to-open link. The image is
     * capped at 900px wide and scales down on narrow reports.
     *
     * <p>Best-effort: a screenshot failure must not mask the real test failure,
     * so problems here are logged and swallowed.
     */
    private void attachScreenshot(ExtentTest test) {
        try {
            WebDriver driver = DriverManager.getDriver();
            String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            String dataUri = "data:image/png;base64," + base64;
            String html = "Screenshot at failure:<br>"
                    + "<img src='" + dataUri + "' "
                    + "style='width:100%; max-width:900px; border:1px solid #ccc; border-radius:4px;'/>";
            test.log(Status.FAIL, html);
        } catch (Exception e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
