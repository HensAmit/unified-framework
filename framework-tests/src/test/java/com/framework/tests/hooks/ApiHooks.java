package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.api.auth.AuthManager;
import com.framework.common.config.ConfigManager;
import com.framework.common.context.TestContext;
import com.framework.common.report.ExtentManager;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;

/**
 * Cucumber hooks for API scenarios.
 *
 * <p>{@link #beforeScenario(Scenario)} ({@code @Before}, order 0):
 * <ol>
 *   <li>Records scenario name and tags into {@link TestContext}.</li>
 *   <li>Creates an Extent test node and registers it on this thread.</li>
 *   <li>Selects the auth mode: scenarios tagged {@code @userAuth} use the
 *       refresh-token (user) flow; all others use client credentials.</li>
 *   <li>Seeds {@code userId} into scenarioVars from config, so write scenarios
 *       can reference {@code ${userId}} in endpoints.</li>
 *   <li>Eagerly resolves the token so auth failures surface at scenario start.</li>
 * </ol>
 *
 * <p>Two {@code @After} hooks run in decreasing order: soft-assertion flush
 * (order 100) before the reporting hook (order 0), so {@link Scenario#isFailed()}
 * reflects assertion failures by the time reporting reads it.
 */
public class ApiHooks {

    private static final Logger log = LogUtils.getLogger(ApiHooks.class);

    private static final String USER_AUTH_TAG = "@userAuth";

    private final TestContext ctx;
    private final AuthManager authManager;
    private final AssertionService assertions;

    public ApiHooks(TestContext ctx, AuthManager authManager, AssertionService assertions) {
        this.ctx = ctx;
        this.authManager = authManager;
        this.assertions = assertions;
    }

    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {
        ctx.setScenarioName(scenario.getName());
        ctx.getScenarioTags().addAll(scenario.getSourceTagNames());

        ExtentTest test = ExtentManager.get().createTest(scenario.getName());
        scenario.getSourceTagNames().forEach(test::assignCategory);
        ExtentTestManager.set(test);

        // Select auth mode based on the @userAuth tag.
        boolean needsUserAuth = scenario.getSourceTagNames().contains(USER_AUTH_TAG);
        authManager.setMode(needsUserAuth
                ? AuthManager.AuthMode.USER
                : AuthManager.AuthMode.CLIENT_CREDENTIALS);

        // Make the configured Spotify user id available as ${userId} in steps.
        String userId = ConfigManager.get().userId();
        if (userId != null && !userId.isBlank()) {
            ctx.getScenarioVars().put("userId", userId);
        }

        log.info("Scenario START: {} {} (auth={})",
                scenario.getName(), scenario.getSourceTagNames(), authManager.getMode());

        // Resolve the token up-front so misconfiguration fails fast.
        ctx.setAuthToken(authManager.getToken());
    }

    @After(order = 100)
    public void flushSoftAssertions(Scenario scenario) {
        if (assertions.hasFailures()) {
            assertions.assertAll();
        }
    }

    @After(order = 0)
    public void afterScenario(Scenario scenario) {
        ExtentTest test = ExtentTestManager.get();

        if (scenario.isFailed()) {
            test.log(Status.FAIL, "Scenario failed: " + scenario.getName());
            attachRequestResponseLogs(test);
        } else {
            test.log(Status.PASS, "Scenario passed: " + scenario.getName());
        }

        log.info("Scenario END:   {} -> {}", scenario.getName(), scenario.getStatus());

        ExtentManager.flush();
        ExtentTestManager.remove();
    }

    private void attachRequestResponseLogs(ExtentTest test) {
        String req = ctx.getRequestLog();
        String resp = ctx.getResponseLog();
        if (req != null) {
            test.info("<details><summary>Request</summary><pre>" + escape(req) + "</pre></details>");
        }
        if (resp != null) {
            test.info("<details><summary>Response</summary><pre>" + escape(resp) + "</pre></details>");
        }
    }

    private static String escape(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
