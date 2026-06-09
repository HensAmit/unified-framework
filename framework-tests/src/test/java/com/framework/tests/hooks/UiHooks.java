package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.common.context.TestContext;
import com.framework.common.report.ExtentManager;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.utils.LogUtils;
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
 * (embedded as base64 in the report) — captured BEFORE quitting the browser.
 * The driver is always quit afterwards to avoid leaking browser processes;
 * "skip cleanup on failure" applies to test data, not the browser session.
 */
public class UiHooks {

    private static final Logger log = LogUtils.getLogger(UiHooks.class);

    private final TestContext ctx;

    public UiHooks(TestContext ctx) {
        this.ctx = ctx;
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

        DriverManager.quitDriver();
        ExtentManager.flush();
        ExtentTestManager.remove();
    }

    private void logStackTrace(ExtentTest test) {
        String trace = ctx.getFailureStackTrace();
        if (trace == null || trace.isBlank()) {
            return;
        }
        test.log(Status.FAIL, "<b>Stack trace:</b><pre>" + escape(trace) + "</pre>");
    }

    /**
     * Captures the current browser screenshot as base64 and embeds it in the
     * Extent report. Best-effort: a screenshot failure must not mask the real
     * test failure, so problems here are logged and swallowed.
     */
    private void attachScreenshot(ExtentTest test) {
        try {
            WebDriver driver = DriverManager.getDriver();
            String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            test.fail("Screenshot at failure:",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
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
