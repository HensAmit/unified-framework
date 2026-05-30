package com.framework.api.auth;

import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.utils.LogUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Resolves an auth token for the configured API.
 *
 * <p>Phase 3 supports only OAuth2 client credentials flow (Spotify's
 * server-to-server flow): POSTs to the configured token URL with
 * {@code grant_type=client_credentials} and HTTP Basic auth carrying
 * {@code client_id:client_secret}. The response contains {@code access_token}
 * and {@code expires_in} (seconds).
 *
 * <p>Tokens are cached in a <strong>process-wide</strong> {@link TokenCache}
 * (the {@code SHARED_CACHE} static field). This is deliberate: PicoContainer
 * creates a new {@code AuthManager} per scenario, but we don't want each
 * scenario to refetch a token from Spotify. Sharing the cache means the first
 * scenario fetches, the rest reuse — including across parallel threads, since
 * {@code TokenCache} is internally thread-safe.
 *
 * <p>Other auth types (Bearer/Basic/API Key) will plug in here in later phases
 * via a strategy switch on {@link AppConfig#authType()}.
 */
public class AuthManager {

    private static final Logger log = LogUtils.getLogger(AuthManager.class);

    /**
     * Process-wide token cache, shared across all PicoContainer scenarios.
     * Static to avoid per-scenario token refetching.
     */
    private static final TokenCache SHARED_CACHE = new TokenCache();

    private final AppConfig config;

    /**
     * PicoContainer-injectable constructor. {@link ConfigManager} is a singleton
     * so we resolve it directly rather than receiving it via injection.
     */
    public AuthManager() {
        this.config = ConfigManager.get();
    }

    /**
     * Returns a valid access token for the configured auth scheme.
     * Fetches and caches a new token if no cached entry is valid.
     */
    public String getToken() {
        String authType = config.authType();
        if (!"oauth2".equalsIgnoreCase(authType)) {
            throw new UnsupportedOperationException(
                    "Phase 3 only supports auth.type=oauth2. Got: " + authType);
        }
        return getOAuth2ClientCredentialsToken();
    }

    private String getOAuth2ClientCredentialsToken() {
        String clientId = require("api.auth.client.id", config.clientId());
        String clientSecret = require("api.auth.client.secret", config.clientSecret());

        String cached = SHARED_CACHE.get(clientId);
        if (cached != null) {
            log.debug("Reusing cached OAuth2 token for client {}", maskedClientId(clientId));
            return cached;
        }

        log.info("Requesting new OAuth2 token from {}", config.tokenUrl());
        Response response = RestAssured.given()
                .auth().preemptive().basic(clientId, clientSecret)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .when()
                .post(config.tokenUrl());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Token endpoint returned " + response.statusCode()
                            + ": " + response.body().asString());
        }

        String token = response.jsonPath().getString("access_token");
        int expiresInSeconds = response.jsonPath().getInt("expires_in");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Token endpoint returned 200 but no access_token in body: "
                            + response.body().asString());
        }

        SHARED_CACHE.put(clientId, token, Duration.ofSeconds(expiresInSeconds));
        log.info("OAuth2 token acquired (expires in {}s)", expiresInSeconds);
        return token;
    }

    /**
     * Returns {@code value}, or throws if it's blank — used to fail fast with
     * a clear error when credentials are not supplied at runtime.
     */
    private static String require(String configKey, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required config '" + configKey + "' is not set. "
                            + "Supply via -D" + configKey + "=... or an env-specific properties file.");
        }
        return value;
    }

    /** Returns the first 4 chars of the client ID for logging, never the full value. */
    private static String maskedClientId(String clientId) {
        return clientId.length() <= 4 ? "****" : clientId.substring(0, 4) + "****";
    }
}
