package com.framework.common.config;

import org.aeonbits.owner.ConfigFactory;

/**
 * Singleton accessor for {AppConfig}.
 *
 * <p>Uses the initialization-on-demand holder idiom for thread-safe lazy loading
 * without explicit synchronization. The JVM guarantees the {@code Holder} class
 * is loaded once, on first access.
 *
 * <p>Typical use:
 * <pre>{@code
 * String url = ConfigManager.get().apiBaseUrl();
 * }</pre>
 *
 * <p>The {@code env} system property selects which environment file to load.
 * If not set on the command line, {@code dev} is used.
 */
public final class ConfigManager {

    private ConfigManager() {
        // utility class — no instances
    }

    /**
     * Returns the singleton AppConfig instance.
     */
    public static AppConfig get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        // Default 'env' to 'dev' if not provided on the command line.
        // OWNER reads this from system properties when resolving ${env} in @Sources.
        static {
            if (System.getProperty("env") == null) {
                System.setProperty("env", "dev");
            }
        }

        private static final AppConfig INSTANCE = ConfigFactory.create(AppConfig.class);
    }
}
