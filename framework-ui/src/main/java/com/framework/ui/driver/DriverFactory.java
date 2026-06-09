package com.framework.ui.driver;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Builds a configured {@link WebDriver} from {@link AppConfig}.
 *
 * <p>Stateless factory — it constructs and returns a driver; it does not hold or
 * manage it (that's {@link DriverManager}'s job). The browser and headless mode
 * are config-driven, so a run can switch between headed local debugging and
 * headless CI via {@code -Dui.headless=true} without code changes.
 *
 * <p>Phase 5 supports Chrome only; additional browsers slot into the switch in a
 * later phase. {@link WebDriverManager} resolves the driver binary so no manual
 * driver installation is needed.
 */
public final class DriverFactory {

    private static final Logger log = LogUtils.getLogger(DriverFactory.class);

    private DriverFactory() {
        // utility — no instances
    }

    /** Creates a new WebDriver for the configured browser. */
    public static WebDriver createDriver() {
        AppConfig config = ConfigManager.get();
        String browser = config.uiBrowser() == null ? "chrome" : config.uiBrowser().trim().toLowerCase();
        log.info("Creating WebDriver: browser={}, headless={}", browser, config.uiHeadless());

        return switch (browser) {
            case "chrome" -> createChrome(config.uiHeadless());
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Phase 5 supports: chrome.");
        };
    }

    private static WebDriver createChrome(boolean headless) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        // Flags that keep Chrome stable in containers/CI and give a consistent viewport.
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080");

        return new ChromeDriver(options);
    }
}
