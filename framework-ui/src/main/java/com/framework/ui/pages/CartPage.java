package com.framework.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for the SauceDemo cart page. Site-specific.
 */
public class CartPage extends BasePage {

    private static final By CART_ITEM = By.className("cart_item");
    private static final By CHECKOUT_BUTTON = By.id("checkout");
    private static final By CONTINUE_SHOPPING = By.id("continue-shopping");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Number of line items currently in the cart. */
    public int getItemCount() {
        return findAll(CART_ITEM).size();
    }

    /** True if a product with the given name is listed in the cart. */
    public boolean containsProduct(String productName) {
        By item = By.xpath(
                "//div[contains(@class,'cart_item')]"
                        + "//div[contains(@class,'inventory_item_name')][normalize-space()='" + productName + "']");
        return isDisplayed(item);
    }

    public void checkout() {
        click(CHECKOUT_BUTTON);
    }

    public void continueShopping() {
        click(CONTINUE_SHOPPING);
    }
}
