package com.framework.tests.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG runner for database scenarios. Points at {@code features/db} and glues
 * the DB steps plus the shared hooks package — only {@code DbHooks} (@db) fires
 * for these scenarios, thanks to tag-scoping.
 */
@CucumberOptions(
        features = "src/test/resources/features/db",
        glue = {"com.framework.tests.steps.db", "com.framework.tests.hooks"},
        plugin = {"pretty"}
)
public class DbTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
