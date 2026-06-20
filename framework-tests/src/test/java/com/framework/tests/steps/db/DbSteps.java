package com.framework.tests.steps.db;

import com.framework.common.service.AssertionService;
import com.framework.common.service.PlaceholderResolver;
import com.framework.db.service.DbService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;

import java.util.List;
import java.util.Map;

/**
 * Database-assertion step definitions, mirroring the API/UI assertion style:
 * thin steps that resolve {@code ${...}} placeholders, run a query through
 * {@link DbService}, and assert via the shared (hard) {@link AssertionService}.
 *
 * <p>Reuses the same {@link PlaceholderResolver} the rest of the framework uses
 * (it reads {@code scenarioVars} internally), so a value saved earlier — e.g. an
 * id from an API call — can be referenced inside a SQL string here.
 */
public class DbSteps {

    private final DbService db;
    private final AssertionService assertions;
    private final PlaceholderResolver resolver;

    public DbSteps(DbService db, AssertionService assertions, PlaceholderResolver resolver) {
        this.db = db;
        this.assertions = assertions;
        this.resolver = resolver;
    }

    @Then("the database query {string} should return {int} row(s)")
    public void theQueryShouldReturnRows(String sql, int expectedCount) {
        String resolved = resolver.resolve(sql);
        List<Map<String, Object>> rows = db.query(resolved);
        assertions.assertEquals("row count for [" + resolved + "]", rows.size(), expectedCount);
    }

    @Then("the database query {string} should return:")
    public void theQueryShouldReturn(String sql, DataTable expectations) {
        String resolved = resolver.resolve(sql);
        List<Map<String, Object>> rows = db.query(resolved);

        assertions.assertTrue("query returned at least one row: " + resolved, !rows.isEmpty());
        Map<String, Object> firstRow = rows.get(0);

        // Each table row is { column, expected }; compare as strings so a numeric
        // column and a string expectation line up uniformly.
        for (Map<String, String> expectation : expectations.asMaps()) {
            String column = expectation.get("column");
            String expected = resolver.resolve(expectation.get("expected"));
            Object actual = getIgnoreCase(firstRow, column);
            assertions.assertEquals("DB column [" + column + "]", String.valueOf(actual), expected);
        }
    }

    private static Object getIgnoreCase(Map<String, Object> row, String column) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(column)) {
                return e.getValue();
            }
        }
        return null;
    }
}
