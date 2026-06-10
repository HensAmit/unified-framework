package com.framework.common.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

/**
 * Framework configuration loaded via the OWNER library.
 *
 * <p>Sources are merged in order — first match wins:
 * system properties, then {@code config/${env}.properties}, then
 * {@code config/default.properties}. The {@code ${env}} placeholder resolves
 * from the {@code env} system property (defaults to {@code dev}).
 *
 * <p>Access via {@link ConfigManager#get()} — never instantiate directly.
 */
@LoadPolicy(LoadType.MERGE)
@Sources({
        "system:properties",
        "classpath:config/${env}.properties",
        "classpath:config/default.properties"
})
public interface AppConfig extends Config {

    // -------------------------------------------------------------------------
    // API configuration
    // -------------------------------------------------------------------------

    @Key("api.base.url")
    String apiBaseUrl();

    @Key("api.auth.type")
    @DefaultValue("oauth2")
    String authType();

    @Key("api.auth.token.url")
    String tokenUrl();

    @Key("api.auth.client.id")
    String clientId();

    @Key("api.auth.client.secret")
    String clientSecret();

    @Key("api.auth.refresh.token")
    String refreshToken();

    @Key("api.user.id")
    String userId();

    @Key("api.default.timeout.ms")
    @DefaultValue("5000")
    int apiTimeout();

    // -------------------------------------------------------------------------
    // UI configuration
    // -------------------------------------------------------------------------

    @Key("ui.base.url")
    String uiBaseUrl();

    @Key("ui.browser")
    @DefaultValue("chrome")
    String uiBrowser();

    @Key("ui.headless")
    @DefaultValue("false")
    boolean uiHeadless();

    @Key("ui.timeout.explicit.ms")
    @DefaultValue("10000")
    long uiExplicitTimeoutMs();

    @Key("ui.timeout.pageload.ms")
    @DefaultValue("30000")
    long uiPageLoadTimeoutMs();

    // -------------------------------------------------------------------------
    // Cross-cutting
    // -------------------------------------------------------------------------

    @Key("env")
    @DefaultValue("dev")
    String env();

    @Key("retry.count")
    @DefaultValue("2")
    int retryCount();
}
