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
 * results through the common {@link AssertionService}.
 *
 * <p>Under hard-assert semantics, the row loop stops at the first failing row —
 * the assertion throws and aborts the scenario. The HTTP call for that row has
 * already been captured as an interaction, so the report shows it as the last
 * (failing) interaction.
 *
 * <p>Driven by a DataTable with columns {@code | type | path | expected |}:
 * status, jsonpath, schema, header, responseTime, arraySize, save.
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
            // Hard fail: capture the validator's trace and rethrow.
            assertions.failWith(e);
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

    private Object readPath(String path) {
        DocumentContext doc = ctx.getResponseContext();
        if (doc == null) {
            return null;
        }
        try {
            return doc.read(path);
        } catch (Exception e) {
            return null;
        }
    }
}
