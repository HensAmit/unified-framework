package com.framework.tests.steps.ui;

import com.framework.common.context.TestContext;
import com.framework.common.utils.LogUtils;
import com.framework.ui.data.ExcelDataReader;
import io.cucumber.java.en.Given;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Loads spreadsheet-backed test data into the scenario.
 *
 * <p>The {@code I load test data for "<id>"} step reads the row whose
 * {@code testcase_id} matches from {@code testdata/ui-testdata.xlsx} and copies
 * every column into {@code scenarioVars}. Subsequent steps then read what they
 * need (username, product, firstName, ...) by key — Excel is the single source
 * of the data, and the feature file stays readable.
 */
public class TestDataSteps {

    private static final Logger log = LogUtils.getLogger(TestDataSteps.class);

    private static final String DATA_FILE = "testdata/ui-testdata.xlsx";
    private static final String ID_COLUMN = "testcase_id";

    private final TestContext ctx;

    public TestDataSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("I load test data for {string}")
    public void iLoadTestDataFor(String testcaseId) {
        Map<String, String> row = ExcelDataReader.getRow(DATA_FILE, ID_COLUMN, testcaseId);
        row.forEach((key, value) -> ctx.getScenarioVars().put(key, value));
        log.info("Loaded {} test-data fields for {}", row.size(), testcaseId);
    }
}
