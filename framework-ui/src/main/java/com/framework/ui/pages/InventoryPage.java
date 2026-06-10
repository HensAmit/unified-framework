package com.framework.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for the SauceDemo inventory (products) page — shown after login.
 * Site-specific.
 */
public class InventoryPage extends BasePage {

    private static final By INVENTORY_CONTAINER = By.id("inventory_container");
    private static final By CART_BADGE = By.className("shopping_cart_badge");
    private static final By CART_LINK = By.className("shopping_cart_link");

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    /** True if the products list rendered. */
    public boolean isLoaded() {
        return isDisplayed(INVENTORY_CONTAINER);
    }

    /**
     * Adds a product to the cart by its display name. The locator finds the
     * inventory item whose name matches, then its add/remove button — robust to
     * SauceDemo's per-product button ids.
     */
    public void addProductToCart(String productName) {
        By addButton = By.xpath(
                "//div[contains(@class,'inventory_item')]"
                        + "[.//div[contains(@class,'inventory_item_name')][normalize-space()='" + productName + "']]"
                        + "//button");
        click(addButton);
    }

    /** Cart badge count, or 0 when the badge is absent (empty cart). */
    public int getCartItemCount() {
        List<WebElement> badges = findAll(CART_BADGE);
        if (badges.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(badges.get(0).getText().trim());
    }

    public void openCart() {
        click(CART_LINK);
    }
}
