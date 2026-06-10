package com.framework.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for SauceDemo checkout step one — the "Your Information" form
 * (first name, last name, postal code). Site-specific.
 */
public class CheckoutStepOnePage extends BasePage {

    private static final By FIRST_NAME = By.id("first-name");
    private static final By LAST_NAME = By.id("last-name");
    private static final By POSTAL_CODE = By.id("postal-code");
    private static final By CONTINUE_BUTTON = By.id("continue");
    private static final By ERROR_BANNER = By.cssSelector("[data-test='error']");

    public CheckoutStepOnePage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isDisplayed(FIRST_NAME);
    }

    public void enterInformation(String firstName, String lastName, String postalCode) {
        type(FIRST_NAME, firstName);
        type(LAST_NAME, lastName);
        type(POSTAL_CODE, postalCode);
    }

    public void continueToOverview() {
        click(CONTINUE_BUTTON);
    }

    public String getErrorMessage() {
        return getText(ERROR_BANNER);
    }
}
