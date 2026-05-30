package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.api.auth.AuthManager;
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
 * <p>{@link Before} runs before every scenario:
 * <ol>
 *   <li>Records scenario name and tags into {@link TestContext}.</li>
 *   <li>Creates an Extent test node and registers it on this thread.</li>
 *   <li>Eagerly resolves the OAuth2 token so auth failures surface with a
 *       clear, top-of-scenario error rather than embedded in a step trace.</li>
 * </ol>
 *
 * <p>Two {@link After} hooks run after every scenario, in this order:
 * <ol>
 *   <li>{@link #flushSoftAssertions(Scenario)} (order = 100) — fails the
 *       scenario if any soft assertions were recorded. Must run before the
 *       reporting hook so {@link Scenario#isFailed()} reflects assertion
 *       failures, not just step exceptions.</li>
 *   <li>{@link #afterScenario(Scenario)} (order = 0, default) — writes the
 *       scenario outcome to the Extent report and flushes.</li>
 * </ol>
 *
 * <p>Cucumber runs {@code @After} hooks in <em>decreasing</em> order of the
 * {@code order} parameter — so {@code order = 100} runs <em>before</em>
 * {@code order = 0}. Counter-intuitive but documented Cucumber behaviour.
 *
 * <p>PicoContainer instantiates this hook class per scenario, injecting
 * {@link TestContext}, {@link AuthManager}, and {@link AssertionService}.
 * The injected instances are the same ones the step classes receive — that's
 * the whole point of the scenario-scoped DI container.
 */
public class ApiHooks {

    private static final Logger log = LogUtils.getLogger(ApiHooks.class);

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

        log.info("Scenario START: {} {}", scenario.getName(), scenario.getSourceTagNames());

        // Resolve the token up-front. If auth is misconfigured, fail fast here
        // with a clear error rather than during the first GET.
        String token = authManager.getToken();
        ctx.setAuthToken(token);
    }

    /**
     * Runs <em>before</em> {@link #afterScenario(Scenario)} thanks to the higher
     * {@code order} value. If any soft assertions accumulated during the
     * scenario, this throws — which Cucumber records as a scenario failure
     * before {@code afterScenario} inspects {@link Scenario#isFailed()}.
     */
    @After(order = 100)
    public void flushSoftAssertions(Scenario scenario) {
        if (assertions.hasFailures()) {
            assertions.assertAll();    // throws AssertionError describing every failure
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

        // Phase 3 flushes per scenario. Phase 7 will move this into a TestNG
        // ISuiteListener so it runs once at suite end.
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
