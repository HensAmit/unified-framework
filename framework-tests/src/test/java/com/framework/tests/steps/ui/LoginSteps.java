package com.framework.tests.steps.ui;

import com.framework.common.context.TestContext;
import com.framework.common.service.AssertionService;
import com.framework.common.utils.LogUtils;
import com.framework.ui.driver.DriverManager;
import com.framework.ui.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.logging.log4j.Logger;

/**
 * Step definitions for the SauceDemo login page.
 *
 * <p>Steps stay thin: they create the page object (obtaining the driver from
 * {@link DriverManager}, set up by the UI {@code @Before} hook) and translate
 * Gherkin into page calls. Assertions go through the shared (hard) {@link
 * AssertionService}, so a UI assertion fails fast and reports just like an API
 * one.
 *
 * <p>The {@code loginPage} field is created in the first step and reused by
 * later steps — Cucumber/PicoContainer use one instance of this step class per
 * scenario, so the field persists across the scenario's steps.
 */
public class LoginSteps {

    private static final Logger log = LogUtils.getLogger(LoginSteps.class);

    private final AssertionService assertions;

    private LoginPage loginPage;

    public LoginSteps(TestContext ctx, AssertionService assertions) {
        this.assertions = assertions;
    }

    @Given("I open the SauceDemo login page")
    public void iOpenTheLoginPage() {
        loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.open();
        log.info("Opened login page: {}", loginPage.getCurrentUrl());
    }

    @Then("the login form should be displayed")
    public void theLoginFormShouldBeDisplayed() {
        assertions.assertTrue("login form is displayed", loginPage.isLoaded());
    }

    @Then("the page title should be {string}")
    public void thePageTitleShouldBe(String expected) {
        assertions.assertEquals("page title", loginPage.getPageTitle(), expected);
    }
}
