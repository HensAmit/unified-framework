package com.framework.api.service;

import com.framework.api.utils.RandomDataUtils;
import com.framework.common.context.TestContext;
import com.framework.common.utils.LogUtils;
import com.jayway.jsonpath.DocumentContext;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

/**
 * Creates and deletes pre-condition test data by calling the real API.
 *
 * <p>Pattern: a tagged {@code @Before} hook calls a factory method to create an
 * entity, stores its id on {@link TestContext#getTestDataMap()}, and a tagged
 * {@code @After} hook deletes it (unless the scenario failed, to preserve data
 * for debugging).
 *
 * <h2>Spotify auth caveat</h2>
 * Spotify's <em>write</em> endpoints (create playlist, add tracks, etc.) require
 * a <strong>user</strong> access token from the Authorization Code flow with
 * scopes like {@code playlist-modify-public}. The framework's current
 * {@code AuthManager} uses the Client Credentials flow, which only authorises
 * <em>read</em> endpoints (search, browse, get-by-id).
 *
 * <p>The methods below are therefore <strong>built and structured correctly but
 * not runnable against live Spotify with client credentials</strong> — a real
 * {@code createPlaylist()} call returns 401. They demonstrate the factory pattern
 * and are exercised by unit tests that verify the request payload is assembled
 * correctly. To run them live, implement the Authorization Code token flow in
 * {@code AuthManager} (a focused "Phase 4.5" add-on).
 */
public class TestDataFactory {

    private static final Logger log = LogUtils.getLogger(TestDataFactory.class);

    private final TestContext ctx;
    private final ApiService api;
    private final PayloadService payloadService;

    public TestDataFactory(TestContext ctx, ApiService api, PayloadService payloadService) {
        this.ctx = ctx;
        this.api = api;
        this.payloadService = payloadService;
    }

    /**
     * Creates a playlist via the API and returns its id.
     *
     * <p><strong>Requires a user access token (see class Javadoc).</strong>
     * With client credentials this returns 401.
     */
    public String createPlaylist(String userId) {
        DocumentContext payload = payloadService.loadPayload("payloads/playlist/create-playlist.json");
        payloadService.set(payload, "$.name", "Test Playlist " + RandomDataUtils.alphanumeric(8));

        Response response = api.post("/users/" + userId + "/playlists", payload.jsonString());
        if (response.getStatusCode() != 201) {
            throw new IllegalStateException(
                    "createPlaylist expected 201 but got " + response.getStatusCode()
                            + ": " + response.getBody().asString());
        }
        String playlistId = response.jsonPath().getString("id");
        ctx.getTestDataMap().put("playlistId", playlistId);
        log.info("Created playlist {}", playlistId);
        return playlistId;
    }

    /**
     * Deletes (unfollows) a playlist by id. Spotify has no hard-delete for
     * playlists; unfollowing is the documented removal mechanism.
     *
     * <p><strong>Requires a user access token (see class Javadoc).</strong>
     */
    public void deletePlaylist(String playlistId) {
        if (playlistId == null) {
            log.warn("deletePlaylist called with null id — nothing to clean up");
            return;
        }
        Response response = api.delete("/playlists/" + playlistId + "/followers");
        log.info("Deleted playlist {} (status {})", playlistId, response.getStatusCode());
    }
}
