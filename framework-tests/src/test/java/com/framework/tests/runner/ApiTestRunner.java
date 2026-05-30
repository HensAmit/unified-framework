package com.framework.tests.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG-Cucumber bridge for API scenarios.
 *
 * <p>Extends {@link AbstractTestNGCucumberTests} so TestNG drives execution
 * (parallel config, listeners, retry analyzer) while Cucumber owns scenario
 * parsing, step matching, and hooks.
 *
 * <p>The {@code @CucumberOptions} annotation tells Cucumber which feature
 * files to scan and which packages contain step definitions and hooks. The
 * parameters are equivalent to the JUnit Platform engine's
 * {@code junit-platform.properties} configuration — they just live here
 * because TestNG-driven Cucumber configures itself via annotations.
 *
 * <p>The overridden {@link #scenarios()} method is what enables parallel
 * scenario execution: TestNG sees a {@code @DataProvider(parallel = true)}
 * and runs each scenario row on a separate thread when parallelism is
 * activated in {@code testng-api.xml}. For Phase 3 we leave the suite
 * single-threaded; Phase 7 flips the switch.
 */
@CucumberOptions(
        features = "src/test/resources/features/api",
        glue = { "com.framework.tests.steps.api", "com.framework.tests.hooks" },
        plugin = {
                "pretty",
                "html:target/reports/cucumber.html",
                "json:target/reports/cucumber.json"
        },
        monochrome = true
)
public class ApiTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
