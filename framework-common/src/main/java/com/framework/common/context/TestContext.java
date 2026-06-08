package com.framework.common.context;

import com.jayway.jsonpath.DocumentContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario-scoped shared state, created fresh per scenario by PicoContainer.
 *
 * <p>No static/global state — each scenario gets its own instance, which is what
 * makes parallel execution safe. All step classes, services, and hooks for a
 * scenario receive this same instance via constructor injection.
 *
 * <p>Collections are initialized eagerly so callers never face nulls.
 *
 * <p>The {@code response} and {@code driver} fields are typed as {@link Object}
 * deliberately: {@code framework-common} must not depend on RestAssured or
 * Selenium. The API/UI modules cast them back to their concrete types.
 */
public class TestContext {

    // --- Request/response state (API) ---
    private DocumentContext requestContext;     // current request payload
    private Object response;                     // raw RestAssured Response
    private DocumentContext responseContext;     // parsed response body
    private String authToken;

    // --- Shared scenario data ---
    private final Map<String, Object> scenarioVars;   // for ${variable} chaining
    private final Map<String, String> headers;        // per-scenario request headers
    private final Map<String, Object> testDataMap;    // IDs created in setup

    // --- Scenario metadata ---
    private String scenarioName;
    private final List<String> scenarioTags;

    // --- Captured HTTP interactions (for failure reporting) ---
    // A scenario may make several HTTP calls; we keep them all so the report can
    // show every request/response. Under hard-assert semantics the last entry is
    // the call whose assertion failed (execution stops there).
    private final List<HttpInteraction> httpInteractions;

    // --- Captured failure stack trace (for failure reporting) ---
    // Set by AssertionService at the moment an assertion throws, so the report
    // can show the same trace seen on the console. Stays null when no assertion
    // failed (or the failure came from a non-assertion exception).
    private String failureStackTrace;

    // --- UI state (populated in Phase 5+) ---
    private Object driver;

    public TestContext() {
        this.scenarioVars = new HashMap<>();
        this.headers = new HashMap<>();
        this.testDataMap = new HashMap<>();
        this.scenarioTags = new ArrayList<>();
        this.httpInteractions = new ArrayList<>();
    }

    /** One captured request/response pair from a single HTTP call. */
    public record HttpInteraction(String requestLog, String responseLog) {}

    // --- requestContext ---
    public DocumentContext getRequestContext() { return requestContext; }
    public void setRequestContext(DocumentContext requestContext) { this.requestContext = requestContext; }

    // --- response ---
    public Object getResponse() { return response; }
    public void setResponse(Object response) { this.response = response; }

    // --- responseContext ---
    public DocumentContext getResponseContext() { return responseContext; }
    public void setResponseContext(DocumentContext responseContext) { this.responseContext = responseContext; }

    // --- authToken ---
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    // --- scenarioVars ---
    public Map<String, Object> getScenarioVars() { return scenarioVars; }

    // --- headers ---
    public Map<String, String> getHeaders() { return headers; }

    // --- testDataMap ---
    public Map<String, Object> getTestDataMap() { return testDataMap; }

    // --- scenarioName ---
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    // --- scenarioTags ---
    public List<String> getScenarioTags() { return scenarioTags; }

    // --- httpInteractions ---
    /** Appends one captured request/response pair. Called by the API filter per HTTP call. */
    public void addHttpInteraction(String requestLog, String responseLog) {
        this.httpInteractions.add(new HttpInteraction(requestLog, responseLog));
    }

    /** All captured interactions for this scenario, in call order. */
    public List<HttpInteraction> getHttpInteractions() { return httpInteractions; }

    // --- failureStackTrace ---
    public String getFailureStackTrace() { return failureStackTrace; }
    public void setFailureStackTrace(String failureStackTrace) { this.failureStackTrace = failureStackTrace; }

    // --- driver (UI) ---
    public Object getDriver() { return driver; }
    public void setDriver(Object driver) { this.driver = driver; }
}
