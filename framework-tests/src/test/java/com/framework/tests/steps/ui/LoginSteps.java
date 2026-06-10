package com.framework.tests.steps.ui;

import com.framework.common.context.TestContext;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import com.framework.ui.pages.PageManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.Logger;

/**
 * Login step definitions.
 *
 * <p>Page objects come from the shared {@link PageManager} (one per scenario,
 * injected by PicoContainer), so login and shopping steps operate on the same
 * page instances. Credentials are read from {@code scenarioVars}, populated by
 * the test-data load step — demonstrating Excel-driven data feeding the flow.
 */
public class LoginSteps {

    private static final Logger log = LogUtils.getLogger(LoginSteps.class);

    private final TestContext ctx;
    private final PageManager pages;
    private final AssertionService assertions;

    public LoginSteps(TestContext ctx, PageManager pages, AssertionService assertions) {
        this.ctx = ctx;
        this.pages = pages;
        this.assertions = assertions;
    }

    @Given("I open the SauceDemo login page")
    public void iOpenTheLoginPage() {
        pages.login().open();
        log.info("Opened login page: {}", pages.login().getCurrentUrl());
    }

    @Then("the login form should be displayed")
    public void theLoginFormShouldBeDisplayed() {
        assertions.assertTrue("login form is displayed", pages.login().isLoaded());
    }

    @Then("the page title should be {string}")
    public void thePageTitleShouldBe(String expected) {
        assertions.assertEquals("page title", pages.login().getPageTitle(), expected);
    }

    @When("I log in with the loaded credentials")
    public void iLogInWithLoadedCredentials() {
        String username = String.valueOf(ctx.getScenarioVars().get("username"));
        String password = String.valueOf(ctx.getScenarioVars().get("password"));
        pages.login().login(username, password);
    }
}
