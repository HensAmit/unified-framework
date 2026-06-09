package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.api.auth.AuthManager;
import com.framework.common.config.ConfigManager;
import com.framework.common.context.TestContext;
import com.framework.common.context.TestContext.HttpInteraction;
import com.framework.common.report.ExtentManager;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.utils.LogUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Cucumber hooks for API scenarios.
 *
 * <p>{@code @Before} (order 0): records metadata, creates the Extent node,
 * selects auth mode by the {@code @userAuth} tag, seeds {@code ${userId}}, and
 * eagerly resolves the token so misconfiguration fails fast.
 *
 * <p>Assertions are now hard — a failing assertion throws during the step,
 * Cucumber marks the scenario failed before any {@code @After} runs, so there is
 * no soft-assertion flush hook. The single {@code @After} reports the outcome:
 * on failure it attaches the captured stack trace (set by the AssertionService
 * at throw time) and every request/response interaction (the last of which is
 * the failing call, since execution stops there).
 */
public class ApiHooks {

    private static final Logger log = LogUtils.getLogger(ApiHooks.class);

    private static final String USER_AUTH_TAG = "@userAuth";

    private final TestContext ctx;
    private final AuthManager authManager;

    public ApiHooks(TestContext ctx, AuthManager authManager) {
        this.ctx = ctx;
        this.authManager = authManager;
    }

    @Before(value = "@api", order = 0)
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

    @After("@api")
    public void afterScenario(Scenario scenario) {
        ExtentTest test = ExtentTestManager.get();

        if (scenario.isFailed()) {
            test.log(Status.FAIL, "Scenario failed: " + scenario.getName()
                    + "<br>Location: " + scenario.getUri() + ":" + scenario.getLine());
            logStackTrace(test);
            attachAllInteractions(test);
        } else {
            test.log(Status.PASS, "Scenario passed: " + scenario.getName());
        }

        log.info("Scenario END:   {} -> {}", scenario.getName(), scenario.getStatus());

        ExtentManager.flush();
        ExtentTestManager.remove();
    }

    /**
     * Writes the captured assertion stack trace into the report — the same trace
     * seen on the console. Null when the failure wasn't an assertion (Cucumber's
     * hook API doesn't expose non-assertion throwables to hooks).
     */
    private void logStackTrace(ExtentTest test) {
        String trace = ctx.getFailureStackTrace();
        if (trace == null || trace.isBlank()) {
            return;
        }
        test.log(Status.FAIL, "<b>Stack trace:</b><pre>" + escape(trace) + "</pre>");
    }

    /**
     * Attaches every captured request/response pair, labelled by call order.
     * Under hard-assert semantics the last pair is the failing call.
     */
    private void attachAllInteractions(ExtentTest test) {
        List<HttpInteraction> interactions = ctx.getHttpInteractions();
        if (interactions.isEmpty()) {
            return;
        }
        int i = 1;
        for (HttpInteraction interaction : interactions) {
            test.info("<details><summary>Request #" + i + "</summary><pre>"
                    + escape(interaction.requestLog()) + "</pre></details>");
            test.info("<details><summary>Response #" + i + "</summary><pre>"
                    + escape(interaction.responseLog()) + "</pre></details>");
            i++;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
