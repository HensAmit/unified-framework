package com.framework.tests.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.framework.common.context.TestContext;
import com.framework.common.report.ExtentManager;
import com.framework.common.report.ExtentTestManager;
import com.framework.common.utils.LogUtils;
import com.framework.db.service.DbService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;

/**
 * Cucumber hooks for database scenarios. Scoped to {@code @db} so they fire only
 * for DB scenarios — the same tag-scoping pattern as {@code ApiHooks} (@api) and
 * {@code UiHooks} (@ui), letting all three domains share one glue setup.
 *
 * <p>{@code @Before}: create the Extent node and open the DB connection (which
 * also seeds the schema/data once per JVM). {@code @After}: report the outcome,
 * attach the stack trace on failure, and close the connection.
 *
 * <p>The Extent report is flushed once per suite by {@code ExtentFlushListener}.
 */
public class DbHooks {

    private static final Logger log = LogUtils.getLogger(DbHooks.class);

    private final TestContext ctx;
    private final DbService db;

    public DbHooks(TestContext ctx, DbService db) {
        this.ctx = ctx;
        this.db = db;
    }

    @Before(value = "@db", order = 0)
    public void beforeScenario(Scenario scenario) {
        ctx.setScenarioName(scenario.getName());
        ctx.getScenarioTags().addAll(scenario.getSourceTagNames());

        ExtentTest test = ExtentManager.get().createTest(scenario.getName());
        scenario.getSourceTagNames().forEach(test::assignCategory);
        ExtentTestManager.set(test);

        log.info("Scenario START: {} {}", scenario.getName(), scenario.getSourceTagNames());

        db.connect();
    }

    @After("@db")
    public void afterScenario(Scenario scenario) {
        ExtentTest test = ExtentTestManager.get();

        if (scenario.isFailed()) {
            test.log(Status.FAIL, "Scenario failed: " + scenario.getName()
                    + "<br>Location: " + scenario.getUri() + ":" + scenario.getLine());
            logStackTrace(test);
        } else {
            test.log(Status.PASS, "Scenario passed: " + scenario.getName());
        }

        log.info("Scenario END:   {} -> {}", scenario.getName(), scenario.getStatus());

        db.close();
        ExtentTestManager.remove();
    }

    private void logStackTrace(ExtentTest test) {
        String trace = ctx.getFailureStackTrace();
        if (trace == null || trace.isBlank()) {
            return;
        }
        test.log(Status.FAIL, "<b>Stack trace:</b><pre>" + escape(trace) + "</pre>");
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
