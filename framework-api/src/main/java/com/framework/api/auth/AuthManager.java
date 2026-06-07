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
 * <p>Supports two OAuth2 grant types, selected per scenario:
 * <ul>
 *   <li>{@link AuthMode#CLIENT_CREDENTIALS} — server-to-server flow for read
 *       endpoints (search, browse). No user context. The default.</li>
 *   <li>{@link AuthMode#USER} — refresh-token exchange (Authorization Code flow)
 *       for write endpoints (create/modify playlists). Carries user context.</li>
 * </ul>
 *
 * <p>The caller (typically {@code ApiHooks}, which knows the scenario's tags)
 * sets the mode via {@link #setMode(AuthMode)} before any request. A scenario
 * tagged {@code @userAuth} uses {@link AuthMode#USER}; everything else uses
 * client credentials.
 *
 * <p>Both token types are cached in a <strong>process-wide</strong>
 * {@link TokenCache}, keyed so the two never collide. The first scenario of
 * each type fetches; the rest reuse — including across parallel threads, since
 * {@code TokenCache} is thread-safe.
 *
 * <h2>Refresh-token flow (USER mode)</h2>
 * A long-lived refresh token (obtained once via manual user consent) is
 * exchanged for a short-lived access token via:
 * <pre>
 *   POST /api/token
 *   Authorization: Basic base64(client_id:client_secret)
 *   grant_type=refresh_token&amp;refresh_token=&lt;the refresh token&gt;
 * </pre>
 * Spotify returns a fresh {@code access_token} (and sometimes a new
 * {@code refresh_token}, which we ignore — the original keeps working).
 */
public class AuthManager {

    private static final Logger log = LogUtils.getLogger(AuthManager.class);

    /** Cache keys — distinct so client and user tokens never collide. */
    private static final String CLIENT_CACHE_KEY = "client_credentials";
    private static final String USER_CACHE_KEY = "user_refresh";

    /** Process-wide token cache, shared across all PicoContainer scenarios. */
    private static final TokenCache SHARED_CACHE = new TokenCache();

    private final AppConfig config;

    /** Per-scenario auth mode; defaults to client credentials. */
    private AuthMode mode = AuthMode.CLIENT_CREDENTIALS;

    public AuthManager() {
        this.config = ConfigManager.get();
    }

    /** The two supported grant types. */
    public enum AuthMode {
        CLIENT_CREDENTIALS,
        USER
    }

    /**
     * Selects the auth mode for the current scenario. Called by hooks based on
     * scenario tags before any request is made.
     */
    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public AuthMode getMode() {
        return mode;
    }

    /**
     * Returns a valid access token for the currently selected mode.
     */
    public String getToken() {
        if (!"oauth2".equalsIgnoreCase(config.authType())) {
            throw new UnsupportedOperationException(
                    "Only auth.type=oauth2 is supported. Got: " + config.authType());
        }
        return switch (mode) {
            case CLIENT_CREDENTIALS -> getClientCredentialsToken();
            case USER -> getUserToken();
        };
    }

    // -------------------------------------------------------------------------
    // Client credentials grant (read endpoints)
    // -------------------------------------------------------------------------

    private String getClientCredentialsToken() {
        String clientId = require("api.auth.client.id", config.clientId());
        String clientSecret = require("api.auth.client.secret", config.clientSecret());

        String cached = SHARED_CACHE.get(CLIENT_CACHE_KEY);
        if (cached != null) {
            log.debug("Reusing cached client-credentials token");
            return cached;
        }

        log.info("Requesting new client-credentials token from {}", config.tokenUrl());
        Response response = RestAssured.given()
                .auth().preemptive().basic(clientId, clientSecret)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .when()
                .post(config.tokenUrl());

        return extractAndCache(response, CLIENT_CACHE_KEY);
    }

    // -------------------------------------------------------------------------
    // Refresh-token grant / Authorization Code flow (write endpoints)
    // -------------------------------------------------------------------------

    private String getUserToken() {
        String clientId = require("api.auth.client.id", config.clientId());
        String clientSecret = require("api.auth.client.secret", config.clientSecret());
        String refreshToken = require("api.auth.refresh.token", config.refreshToken());

        String cached = SHARED_CACHE.get(USER_CACHE_KEY);
        if (cached != null) {
            log.debug("Reusing cached user token");
            return cached;
        }

        log.info("Exchanging refresh token for a user access token at {}", config.tokenUrl());
        Response response = RestAssured.given()
                .auth().preemptive().basic(clientId, clientSecret)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "refresh_token")
                .formParam("refresh_token", refreshToken)
                .when()
                .post(config.tokenUrl());

        return extractAndCache(response, USER_CACHE_KEY);
    }

    // -------------------------------------------------------------------------
    // Shared response handling
    // -------------------------------------------------------------------------

    private String extractAndCache(Response response, String cacheKey) {
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

        SHARED_CACHE.put(cacheKey, token, Duration.ofSeconds(expiresInSeconds));
        log.info("Token acquired for '{}' (expires in {}s)", cacheKey, expiresInSeconds);
        return token;
    }

    private static String require(String configKey, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required config '" + configKey + "' is not set. "
                            + "Supply via -D" + configKey + "=... or an env-specific properties file.");
        }
        return value;
    }
}
