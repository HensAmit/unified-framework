package com.framework.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for SauceDemo checkout step two — the order overview with totals
 * and the Finish button. Site-specific.
 */
public class CheckoutStepTwoPage extends BasePage {

    private static final By FINISH_BUTTON = By.id("finish");
    private static final By CANCEL_BUTTON = By.id("cancel");
    private static final By SUMMARY_TOTAL = By.className("summary_total_label");

    public CheckoutStepTwoPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isDisplayed(FINISH_BUTTON);
    }

    /** The "Total: $xx.xx" summary label text. */
    public String getTotalLabel() {
        return getText(SUMMARY_TOTAL);
    }

    public void finish() {
        click(FINISH_BUTTON);
    }

    public void cancel() {
        click(CANCEL_BUTTON);
    }
}
