package com.framework.ui.driver;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Builds a configured {@link WebDriver} from {@link AppConfig}.
 *
 * <p>Two independent, config-driven choices:
 * <ul>
 *   <li><b>Which browser</b> — {@code ui.browser} selects chrome, firefox, or
 *       edge. The matching {@code *Options} object is built once and carries the
 *       headless flag and stability arguments.</li>
 *   <li><b>Where it runs</b> — {@code ui.grid.enabled} decides between a
 *       <em>local</em> driver (this machine) and a <em>remote</em>
 *       {@link RemoteWebDriver} talking to a Selenium Grid hub at
 *       {@code ui.grid.url}.</li>
 * </ul>
 *
 * <p>The two choices are orthogonal: the same {@code Options} object is handed to
 * either a local driver constructor or a {@code RemoteWebDriver}. The return type
 * is the {@link WebDriver} interface, so callers ({@code DriverManager}, page
 * objects, hooks) are identical whether the browser is local or on a Grid node —
 * which is exactly why this remote path touches no other class.
 *
 * <p>Cross-browser testing is sequential: run the suite once per browser via
 * {@code -Dui.browser=firefox}, etc. — not multiple browsers in one run.
 *
 * <p>{@link WebDriverManager} resolves the local driver binary; on a Grid the
 * node already has the driver, so it isn't invoked for the remote path.
 */
public final class DriverFactory {

    private static final Logger log = LogUtils.getLogger(DriverFactory.class);

    private DriverFactory() {
        // utility — no instances
    }

    /** Creates a new WebDriver for the configured browser, local or on a Grid. */
    public static WebDriver createDriver() {
        AppConfig config = ConfigManager.get();
        String browser = config.uiBrowser() == null ? "chrome" : config.uiBrowser().trim().toLowerCase();
        boolean headless = config.uiHeadless();
        boolean grid = config.uiGridEnabled();
        log.info("Creating WebDriver: browser={}, headless={}, grid={}", browser, headless, grid);

        MutableCapabilities options = buildOptions(browser, headless);

        return grid
                ? createRemote(config.uiGridUrl(), options)
                : createLocal(browser, options);
    }

    // -------------------------------------------------------------------------
    // Options — per-browser capability objects (shared by local and remote)
    // -------------------------------------------------------------------------

    private static MutableCapabilities buildOptions(String browser, boolean headless) {
        return switch (browser) {
            case "chrome" -> chromeOptions(headless);
            case "firefox" -> firefoxOptions(headless);
            case "edge" -> edgeOptions(headless);
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Supported: chrome, firefox, edge.");
        };
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1920", "--height=1080");
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        // Edge is Chromium-based, so it takes the same flags as Chrome.
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
        return options;
    }

    // -------------------------------------------------------------------------
    // Local — driver runs on this machine; WebDriverManager resolves the binary
    // -------------------------------------------------------------------------

    private static WebDriver createLocal(String browser, MutableCapabilities options) {
        return switch (browser) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver((ChromeOptions) options);
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver((FirefoxOptions) options);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver((EdgeOptions) options);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Supported: chrome, firefox, edge.");
        };
    }

    // -------------------------------------------------------------------------
    // Remote — driver runs on a Selenium Grid node; we only point at the hub
    // -------------------------------------------------------------------------

    private static WebDriver createRemote(String gridUrl, MutableCapabilities options) {
        URL hub;
        try {
            hub = URI.create(gridUrl).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid grid URL: " + gridUrl, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid grid URL syntax: " + gridUrl, e);
        }
        log.info("Connecting to Selenium Grid at {}", hub);
        return new RemoteWebDriver(hub, options);
    }
}
