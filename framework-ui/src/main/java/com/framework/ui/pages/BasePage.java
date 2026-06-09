package com.framework.ui.pages;

import com.framework.common.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class for all page objects.
 *
 * <p>Provides the common interaction primitives — click, type, read text,
 * visibility checks — each guarded by an <strong>explicit wait</strong>. We use
 * classic {@link By} locators plus {@link WebDriverWait} rather than Selenium's
 * PageFactory/{@code @FindBy}: explicit waits make the synchronization visible
 * and controllable, and avoid the stale-proxy surprises PageFactory can cause.
 *
 * <p>Concrete pages (e.g. {@code LoginPage}) extend this, declare their locators
 * as {@code By} constants, and expose intention-revealing methods built on these
 * primitives.
 *
 * <p>The driver is passed in by the caller (a step), which obtains it from
 * {@code DriverManager}. Pages therefore depend only on a {@link WebDriver},
 * keeping them decoupled from the DI container and the scenario context.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        long timeoutMs = ConfigManager.get().uiExplicitTimeoutMs();
        this.wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));
    }

    // -------------------------------------------------------------------------
    // Wait helpers
    // -------------------------------------------------------------------------

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // -------------------------------------------------------------------------
    // Interaction primitives (all wait-guarded)
    // -------------------------------------------------------------------------

    protected void click(By locator) {
        waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText();
    }

    /**
     * Returns true if the element becomes visible within the explicit-wait
     * window, false if it times out. Used for presence assertions without
     * throwing on absence.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Page-level helpers
    // -------------------------------------------------------------------------

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
