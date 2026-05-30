package com.framework.tests.steps.api;

import com.framework.api.service.ApiService;
import com.framework.common.context.TestContext;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

/**
 * Step definitions shared across all API features. Phase 3 exposes two:
 * <ul>
 *   <li>{@code When I send a GET request to "..."} — issues the call via
 *       {@link ApiService} and stashes the response.</li>
 *   <li>{@code Then the response status code should be N} — soft-asserts the
 *       status. The check is recorded via {@link AssertionService} and
 *       evaluated at scenario end by {@code ApiHooks}.</li>
 * </ul>
 *
 * <p>PicoContainer injects the three collaborators per scenario. All three
 * share the same {@link TestContext} instance, which is how the response
 * stashed by the GET step is later read by the assertion step.
 *
 * <p>Phase 4 will expand this with payload-body steps, header staging,
 * full DataTable-driven assertions, and the {@code save} extraction type.
 */
public class CommonApiSteps {

    private static final Logger log = LogUtils.getLogger(CommonApiSteps.class);

    private final TestContext ctx;
    private final ApiService api;
    private final AssertionService assertions;

    public CommonApiSteps(TestContext ctx, ApiService api, AssertionService assertions) {
        this.ctx = ctx;
        this.api = api;
        this.assertions = assertions;
    }

    @When("I send a GET request to {string}")
    public void iSendAGETRequestTo(String endpoint) {
        api.get(endpoint);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expected) {
        Response response = (Response) ctx.getResponse();
        if (response == null) {
            assertions.fail("No response captured — was a request sent before this assertion?");
            return;
        }
        int actual = response.getStatusCode();
        log.info("Asserting status: expected={}, actual={}", expected, actual);
        assertions.assertEquals("HTTP status code", actual, expected);
    }
}
