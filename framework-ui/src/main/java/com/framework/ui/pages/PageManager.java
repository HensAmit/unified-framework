package com.framework.ui.pages;

import com.framework.ui.driver.DriverManager;

/**
 * Lazily creates and caches page objects for a scenario.
 *
 * <p>Injected into step classes by PicoContainer, which creates one instance per
 * scenario — so all step classes in a scenario share the same page objects, and
 * pages don't leak across scenarios. Each page is built on first request using
 * the current thread's driver from {@link DriverManager}.
 *
 * <p>This is a page-object <em>manager</em>, not Selenium's PageFactory — there's
 * no {@code @FindBy} reflection here, just lazy construction of our explicit-wait
 * page objects. It keeps step code clean: a step calls {@code pages.cart()}
 * rather than {@code new CartPage(DriverManager.getDriver())} each time.
 */
public class PageManager {

    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;
    private CheckoutStepOnePage checkoutStepOnePage;
    private CheckoutStepTwoPage checkoutStepTwoPage;
    private CheckoutCompletePage checkoutCompletePage;

    public LoginPage login() {
        if (loginPage == null) {
            loginPage = new LoginPage(DriverManager.getDriver());
        }
        return loginPage;
    }

    public InventoryPage inventory() {
        if (inventoryPage == null) {
            inventoryPage = new InventoryPage(DriverManager.getDriver());
        }
        return inventoryPage;
    }

    public CartPage cart() {
        if (cartPage == null) {
            cartPage = new CartPage(DriverManager.getDriver());
        }
        return cartPage;
    }

    public CheckoutStepOnePage checkoutStepOne() {
        if (checkoutStepOnePage == null) {
            checkoutStepOnePage = new CheckoutStepOnePage(DriverManager.getDriver());
        }
        return checkoutStepOnePage;
    }

    public CheckoutStepTwoPage checkoutStepTwo() {
        if (checkoutStepTwoPage == null) {
            checkoutStepTwoPage = new CheckoutStepTwoPage(DriverManager.getDriver());
        }
        return checkoutStepTwoPage;
    }

    public CheckoutCompletePage checkoutComplete() {
        if (checkoutCompletePage == null) {
            checkoutCompletePage = new CheckoutCompletePage(DriverManager.getDriver());
        }
        return checkoutCompletePage;
    }
}
