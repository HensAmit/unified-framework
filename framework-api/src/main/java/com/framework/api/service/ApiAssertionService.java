package com.framework.api.service;

import com.framework.common.context.TestContext;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Executes DataTable-driven response assertions.
 *
 * <p>Lives in {@code framework-api} (not {@code framework-common}) because it
 * needs RestAssured's {@link Response} and the JSON Schema validator. It records
 * results through the common {@link AssertionService}, so all failures across a
 * scenario are collected and reported together (soft-assertion semantics).
 *
 * <p>Driven by a DataTable with columns {@code | type | path | expected |}:
 * <ul>
 *   <li>{@code status}       — assert HTTP status code equals expected</li>
 *   <li>{@code jsonpath}     — assert value at {@code $.path}; supports
 *       {@code is not null}, {@code is null}, {@code contains X}, exact match</li>
 *   <li>{@code schema}       — validate body against a schema file in {@code /schemas/}</li>
 *   <li>{@code header}       — assert response header equals expected</li>
 *   <li>{@code responseTime} — assert response time ≤ expected milliseconds</li>
 *   <li>{@code arraySize}    — assert array length at {@code $.path}</li>
 *   <li>{@code save}         — extract value at {@code $.path} into scenarioVars
 *       under the key named in {@code expected} (for {@code ${}} chaining)</li>
 * </ul>
 *
 * <p>{@code expected} values have {@code ${placeholders}} resolved before
 * comparison — except for {@code save}, where {@code expected} is the variable
 * name to store under.
 */
public class ApiAssertionService {

    private static final Logger log = LogUtils.getLogger(ApiAssertionService.class);

    private final TestContext ctx;
    private final AssertionService assertions;
    private final ValueParser valueParser;

    public ApiAssertionService(TestContext ctx, AssertionService assertions, ValueParser valueParser) {
        this.ctx = ctx;
        this.assertions = assertions;
        this.valueParser = valueParser;
    }

    public void assertResponse(DataTable table) {
        Response response = (Response) ctx.getResponse();
        if (response == null) {
            assertions.fail("No response captured — was a request sent before this assertion?");
            return;
        }

        for (Map<String, String> row : table.asMaps()) {
            String type = row.get("type").trim().toLowerCase();
            String path = row.get("path");
            String expected = row.get("expected");

            switch (type) {
                case "status"       -> assertStatus(response, expected);
                case "jsonpath"     -> assertJsonPath(path, expected);
                case "schema"       -> assertSchema(response, path);
                case "header"       -> assertHeader(response, path, expected);
                case "responsetime" -> assertResponseTime(response, expected);
                case "arraysize"    -> assertArraySize(path, expected);
                case "save"         -> save(path, expected);
                default -> throw new IllegalArgumentException(
                        "Unknown assertion type '" + type + "'.");
            }
        }
    }

    private void assertStatus(Response response, String expected) {
        int expectedCode = Integer.parseInt(expected.trim());
        assertions.assertEquals("HTTP status code", response.getStatusCode(), expectedCode);
    }

    private void assertJsonPath(String path, String rawExpected) {
        Object actual = readPath(path);
        String expected = valueParser.resolveString(rawExpected);

        if ("is not null".equalsIgnoreCase(expected)) {
            assertions.assertNotNull("jsonpath " + path + " not null", actual);
        } else if ("is null".equalsIgnoreCase(expected)) {
            assertions.assertNull("jsonpath " + path + " is null", actual);
        } else if (expected != null && expected.toLowerCase().startsWith("contains ")) {
            String needle = expected.substring("contains ".length());
            assertions.assertContains("jsonpath " + path + " contains", String.valueOf(actual), needle);
        } else {
            assertions.assertEquals("jsonpath " + path, String.valueOf(actual), expected);
        }
    }

    private void assertSchema(Response response, String schemaFile) {
        try {
            response.then().assertThat()
                    .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + schemaFile));
        } catch (AssertionError e) {
            // Convert the hard failure into a recorded soft failure.
            assertions.fail("Schema validation failed for " + schemaFile + ": " + e.getMessage());
        }
    }

    private void assertHeader(Response response, String headerName, String rawExpected) {
        String actual = response.getHeader(headerName);
        String expected = valueParser.resolveString(rawExpected);
        assertions.assertEquals("header " + headerName, actual, expected);
    }

    private void assertResponseTime(Response response, String expected) {
        long maxMs = Long.parseLong(expected.trim());
        assertions.assertLessThanOrEqualTo("response time (ms)", response.getTime(), maxMs);
    }

    private void assertArraySize(String path, String expected) {
        int expectedSize = Integer.parseInt(expected.trim());
        Object value = readPath(path);
        if (value instanceof List<?> list) {
            assertions.assertEquals("arraySize " + path, list.size(), expectedSize);
        } else {
            assertions.fail("arraySize " + path + " — value at path is not an array: " + value);
        }
    }

    private void save(String path, String varName) {
        Object value = readPath(path);
        ctx.getScenarioVars().put(varName, value);
        log.info("Saved {} = {} into scenarioVars", varName, value);
    }

    /**
     * Reads a value from the parsed response context using Jayway JsonPath.
     * Returns null if the path is absent or no response body was parsed.
     */
    private Object readPath(String path) {
        DocumentContext doc = ctx.getResponseContext();
        if (doc == null) {
            return null;
        }
        try {
            return doc.read(path);
        } catch (Exception e) {
            // Path not present — treat as null so 'is null' assertions can pass.
            return null;
        }
    }
}
