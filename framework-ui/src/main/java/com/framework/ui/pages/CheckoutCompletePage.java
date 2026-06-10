package com.framework.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for the SauceDemo order-complete page — the confirmation shown
 * after finishing checkout. Site-specific.
 */
public class CheckoutCompletePage extends BasePage {

    private static final By COMPLETE_HEADER = By.className("complete-header");

    public CheckoutCompletePage(WebDriver driver) {
        super(driver);
    }

    public boolean isComplete() {
        return isDisplayed(COMPLETE_HEADER);
    }

    /** The confirmation header text, e.g. "Thank you for your order!". */
    public String getCompleteHeader() {
        return getText(COMPLETE_HEADER);
    }
}
