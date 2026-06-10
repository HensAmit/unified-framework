package com.framework.tests.steps.ui;

import com.framework.common.context.TestContext;
import com.framework.common.service.AssertionService;
import com.framework.ui.pages.PageManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the shopping flow: inventory → cart → checkout →
 * confirmation. Data-driven values (product name, checkout details) are read
 * from {@code scenarioVars}, populated by the test-data load step.
 *
 * <p>All page objects come from the shared {@link PageManager}; assertions go
 * through the hard {@link AssertionService}, so a UI check fails fast and reports
 * with a stack trace and failure screenshot exactly like the API side.
 */
public class ShoppingSteps {

    private final TestContext ctx;
    private final PageManager pages;
    private final AssertionService assertions;

    public ShoppingSteps(TestContext ctx, PageManager pages, AssertionService assertions) {
        this.ctx = ctx;
        this.pages = pages;
        this.assertions = assertions;
    }

    @Then("the inventory page should be displayed")
    public void theInventoryPageShouldBeDisplayed() {
        assertions.assertTrue("inventory page is displayed", pages.inventory().isLoaded());
    }

    @When("I add the test-data product to the cart")
    public void iAddTheTestDataProductToTheCart() {
        String product = String.valueOf(ctx.getScenarioVars().get("product"));
        pages.inventory().addProductToCart(product);
    }

    @Then("the cart badge should show {int} item(s)")
    public void theCartBadgeShouldShow(int expectedCount) {
        assertions.assertEquals("cart badge count", pages.inventory().getCartItemCount(), expectedCount);
    }

    @When("I open the cart")
    public void iOpenTheCart() {
        pages.inventory().openCart();
    }

    @Then("the cart should contain the test-data product")
    public void theCartShouldContainTheProduct() {
        String product = String.valueOf(ctx.getScenarioVars().get("product"));
        assertions.assertTrue("cart contains " + product, pages.cart().containsProduct(product));
    }

    @When("I proceed to checkout")
    public void iProceedToCheckout() {
        pages.cart().checkout();
    }

    @When("I enter the checkout information from test data")
    public void iEnterTheCheckoutInformation() {
        String firstName = String.valueOf(ctx.getScenarioVars().get("firstName"));
        String lastName = String.valueOf(ctx.getScenarioVars().get("lastName"));
        String postalCode = String.valueOf(ctx.getScenarioVars().get("postalCode"));
        pages.checkoutStepOne().enterInformation(firstName, lastName, postalCode);
        pages.checkoutStepOne().continueToOverview();
    }

    @When("I finish the checkout")
    public void iFinishTheCheckout() {
        pages.checkoutStepTwo().finish();
    }

    @Then("the order confirmation should be displayed")
    public void theOrderConfirmationShouldBeDisplayed() {
        assertions.assertTrue("order complete page shown", pages.checkoutComplete().isComplete());
        assertions.assertContains("confirmation message",
                pages.checkoutComplete().getCompleteHeader(), "Thank you for your order");
    }
}
