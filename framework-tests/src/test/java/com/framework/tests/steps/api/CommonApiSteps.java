package com.framework.tests.steps.api;

import com.framework.api.service.ApiAssertionService;
import com.framework.api.service.ApiService;
import com.framework.api.service.PayloadService;
import com.framework.api.service.ValueParser;
import com.framework.common.context.TestContext;
import com.framework.common.report.ReportLog;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

/**
 * Step definitions shared across all API features.
 *
 * <p>Phase 4 expands the Phase 3 surface (GET + status assertion) with:
 * <ul>
 *   <li>request header staging</li>
 *   <li>payload template loading + DataTable-driven mutation</li>
 *   <li>POST/PUT/PATCH/DELETE verbs</li>
 *   <li>the full DataTable-driven response assertion ({@code I assert the response})</li>
 *   <li>standalone save-for-chaining step</li>
 * </ul>
 *
 * <p>Steps stay thin: they translate Gherkin into calls on services. All real
 * work lives in {@link ApiService}, {@link PayloadService}, and
 * {@link ApiAssertionService}. PicoContainer injects everything, sharing one
 * {@link TestContext} across all of them.
 */
public class CommonApiSteps {

    private static final Logger log = LogUtils.getLogger(CommonApiSteps.class);

    private final TestContext ctx;
    private final ApiService api;
    private final PayloadService payloadService;
    private final ApiAssertionService apiAssertions;
    private final AssertionService assertions;
    private final ValueParser valueParser;

    public CommonApiSteps(TestContext ctx,
                          ApiService api,
                          PayloadService payloadService,
                          ApiAssertionService apiAssertions,
                          AssertionService assertions,
                          ValueParser valueParser) {
        this.ctx = ctx;
        this.api = api;
        this.payloadService = payloadService;
        this.apiAssertions = apiAssertions;
        this.assertions = assertions;
        this.valueParser = valueParser;
    }

    // -------------------------------------------------------------------------
    // Headers
    // -------------------------------------------------------------------------

    @Given("the request has header {string} with value {string}")
    public void theRequestHasHeader(String name, String value) {
        ctx.getHeaders().put(name, valueParser.resolveString(value));
    }

    // -------------------------------------------------------------------------
    // Payload
    // -------------------------------------------------------------------------

    @Given("I load the payload {string}")
    public void iLoadThePayload(String classpathPath) {
        ReportLog.info("Loading payload template: " + classpathPath);
        payloadService.loadPayload(classpathPath);
    }

    @Given("I update the request payload:")
    public void iUpdateTheRequestPayload(DataTable table) {
        payloadService.update(table);
        ReportLog.pass("Updated the Request payload successfully");
    }

    // -------------------------------------------------------------------------
    // HTTP verbs
    // -------------------------------------------------------------------------

    @When("I send a GET request to {string}")
    public void iSendAGETRequestTo(String endpoint) {
        api.get(valueParser.resolveString(endpoint));
    }

    @When("I send a POST request to {string}")
    public void iSendAPOSTRequestTo(String endpoint) {
        String body = currentBody();
        api.post(valueParser.resolveString(endpoint), body);
    }

    @When("I send a PUT request to {string}")
    public void iSendAPUTRequestTo(String endpoint) {
        String body = currentBody();
        api.put(valueParser.resolveString(endpoint), body);
    }

    @When("I send a PATCH request to {string}")
    public void iSendAPATCHRequestTo(String endpoint) {
        String body = currentBody();
        api.patch(valueParser.resolveString(endpoint), body);
    }

    @When("I send a DELETE request to {string}")
    public void iSendADELETERequestTo(String endpoint) {
        api.delete(valueParser.resolveString(endpoint));
    }

    // -------------------------------------------------------------------------
    // Assertions
    // -------------------------------------------------------------------------

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expected) {
        Response response = (Response) ctx.getResponse();
        if (response == null) {
            assertions.fail("No response captured — was a request sent before this assertion?");
            return;
        }
        log.info("Asserting status: expected={}, actual={}", expected, response.getStatusCode());
        assertions.assertEquals("HTTP status code", response.getStatusCode(), expected);
    }

    @Then("I assert the response:")
    public void iAssertTheResponse(DataTable table) {
        apiAssertions.assertResponse(table);
    }

    // -------------------------------------------------------------------------
    // Chaining
    // -------------------------------------------------------------------------

    @Given("I save response field {string} as {string}")
    public void iSaveResponseFieldAs(String path, String varName) {
        Object value = ctx.getResponseContext() == null ? null : ctx.getResponseContext().read(path);
        ctx.getScenarioVars().put(varName, value);
        log.info("Saved {} = {} into scenarioVars", varName, value);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the current request payload as a JSON string, or null if none loaded. */
    private String currentBody() {
        return ctx.getRequestContext() == null ? null : ctx.getRequestContext().jsonString();
    }
}
