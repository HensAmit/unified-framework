package com.framework.common.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

/**
 * Framework configuration loaded via the OWNER library.
 *
 * <p>Sources are merged in order — first match wins:
 * <ol>
 *   <li>{@code system:properties} — JVM system properties ({@code -Dapi.base.url=...})</li>
 *   <li>{@code classpath:config/${env}.properties} — environment-specific overrides</li>
 *   <li>{@code classpath:config/default.properties} — fallback defaults</li>
 * </ol>
 *
 * <p>The {@code ${env}} placeholder is resolved at load time from the {@code env}
 * system property (defaults to {@code dev} if not provided).
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

    // Phase 4.5 — Authorization Code (user) flow.
    // The long-lived refresh token obtained via one-time manual user consent.
    @Key("api.auth.refresh.token")
    String refreshToken();

    // The Spotify user ID (alphanumeric account id) used for user-scoped
    // endpoints like POST /users/{id}/playlists.
    @Key("api.user.id")
    String userId();

    @Key("api.default.timeout.ms")
    @DefaultValue("5000")
    int apiTimeout();

    // -------------------------------------------------------------------------
    // Cross-cutting
    // -------------------------------------------------------------------------

    @Key("env")
    @DefaultValue("dev")
    String env();

    @Key("retry.count")
    @DefaultValue("2")
    int retryCount();

    // UI keys are added in Phase 5.
}
