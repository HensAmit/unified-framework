package com.framework.ui.pages;

import com.framework.common.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for the SauceDemo login page (the application's landing page).
 *
 * <p><strong>Site-specific.</strong> The locators and {@code open()} URL target
 * SauceDemo. To point the framework at a different site, this class and its
 * locators change; the infrastructure ({@code BasePage}, {@code DriverFactory},
 * {@code DriverManager}, hooks, runner) does not.
 */
public class LoginPage extends BasePage {

    // Locators — classic By constants (no @FindBy).
    private static final By USERNAME_FIELD = By.id("user-name");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By ERROR_BANNER = By.cssSelector("[data-test='error']");

    private final String baseUrl;

    public LoginPage(WebDriver driver) {
        super(driver);
        this.baseUrl = ConfigManager.get().uiBaseUrl();
    }

    /** Navigates the browser to the login page. */
    public void open() {
        driver.get(baseUrl);
    }

    /** True if the login form (username field + login button) is present. */
    public boolean isLoaded() {
        return isDisplayed(USERNAME_FIELD) && isDisplayed(LOGIN_BUTTON);
    }

    /** Enters credentials and submits. */
    public void login(String username, String password) {
        type(USERNAME_FIELD, username);
        type(PASSWORD_FIELD, password);
        click(LOGIN_BUTTON);
    }

    /** The error banner text, if a failed login produced one. */
    public String getErrorMessage() {
        return getText(ERROR_BANNER);
    }
}
