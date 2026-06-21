package com.framework.tests.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG runner for UI scenarios.
 *
 * <p>Points at {@code features/ui} and glues the UI steps plus the hooks
 * package. Both hook classes ({@code ApiHooks}, {@code UiHooks}) live in that
 * package, but each is tag-scoped ({@code @api} / {@code @ui}), so only
 * {@code UiHooks} fires for these scenarios.
 *
 * <p>The {@code @DataProvider(parallel = true)} override enables parallel
 * scenario execution in Phase 7; thread count is governed by the TestNG suite.
 *
 * <p>No {@code tags} are set in {@link CucumberOptions} so that the
 * {@code -Dcucumber.filter.tags} system property fully controls filtering at
 * run time.
 */
@CucumberOptions(
        features = "src/test/resources/features/ui",
        glue = {"com.framework.tests.steps.ui", "com.framework.tests.steps.db", "com.framework.tests.hooks"},
        plugin = {"pretty"}
)
public class UiTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
