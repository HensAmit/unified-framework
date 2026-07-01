package com.framework.ui.pages;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.ui.report.UiReportLog;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Base class for all page objects.
 *
 * <p>Provides interaction primitives — click, type, read text, presence checks —
 * each guarded by an <strong>explicit wait</strong>. Uses classic {@link By}
 * locators plus {@link WebDriverWait} (not PageFactory/{@code @FindBy}).
 *
 * <p><strong>Condition-specific timeouts.</strong> Two pre-built waits cover the
 * common cases: {@code wait} (element-level, the standard explicit timeout) and
 * {@code pageWait} (longer, for page transitions like URL changes). A
 * per-call overload lets a page supply a custom timeout when a specific
 * condition needs more or less patience than the defaults.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;       // standard element-level waits
    protected final WebDriverWait pageWait;   // longer waits for page transitions

    protected BasePage(WebDriver driver) {
        AppConfig config = ConfigManager.get();
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofMillis(config.uiExplicitTimeoutMs()));
        this.pageWait = new WebDriverWait(driver, Duration.ofMillis(config.uiPageLoadTimeoutMs()));
    }

    // -------------------------------------------------------------------------
    // Wait helpers (condition-specific)
    // -------------------------------------------------------------------------

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Visibility wait with a caller-supplied timeout, for conditions that need a non-default window. */
    protected WebElement waitForVisible(By locator, long timeoutMs) {
        return new WebDriverWait(driver, Duration.ofMillis(timeoutMs))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Waits (page-transition timeout) for the current URL to contain the fragment. */
    protected void waitForUrlContains(String fragment) {
        pageWait.until(ExpectedConditions.urlContains(fragment));
    }

    /** Waits for an element to disappear (e.g. a spinner, or a removed cart badge). */
    protected boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // -------------------------------------------------------------------------
    // Interaction primitives (all wait-guarded)
    // -------------------------------------------------------------------------

    protected void click(By locator) {
        UiReportLog.infoWithScreenshot("Before clicking: " + locator);
        waitForClickable(locator).click();
        UiReportLog.infoWithScreenshot("After clicking: " + locator);
    }

    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
        UiReportLog.info("Entered text " + text + " in " + locator);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText();
    }

    /** True if visible within the explicit-wait window, false on timeout. */
    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Raw element lookup without waiting — for counts/presence checks that tolerate absence. */
    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
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
