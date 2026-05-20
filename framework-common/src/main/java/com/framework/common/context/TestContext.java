package com.framework.common.context;

import com.jayway.jsonpath.DocumentContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The single, shared state object for a Cucumber scenario.
 *
 * <p>PicoContainer instantiates one {@code TestContext} per scenario and injects
 * it into every step class, hook, page object, and service that declares it as
 * a constructor parameter. There is no static state — each scenario gets its
 * own context, which is exactly what makes parallel execution safe.
 *
 * <p>Fields are split into three groups:
 * <ul>
 *   <li><b>API fields</b> — request/response state, auth token, captured logs</li>
 *   <li><b>UI fields</b> — WebDriver, current page name, test data row from Excel</li>
 *   <li><b>Shared fields</b> — placeholder vars, headers, scenario metadata</li>
 * </ul>
 *
 * <p>Most fields are populated by services and hooks during the scenario.
 * Collection fields are initialised to empty collections so callers never
 * encounter nulls; they may add to them freely.
 *
 * <p><strong>On the use of {@link Object} for {@code response} and {@code driver}:</strong>
 * {@code framework-common} is the lowest module and intentionally has no
 * dependency on RestAssured or Selenium. Storing those as {@code Object}
 * keeps the dependency graph clean. API and UI services know the real type
 * and cast on retrieval. JsonPath's {@link DocumentContext} is included as a
 * direct type because JsonPath {@code is} part of the common surface — both
 * API and UI code may need to work with parsed JSON.
 *
 * <p>The class is intentionally a simple mutable bean — no encapsulation
 * heroics, because PicoContainer needs trivial wiring and step code reads
 * cleaner with direct getter/setter access.
 */
public class TestContext {

    // -------------------------------------------------------------------------
    // API state
    // -------------------------------------------------------------------------

    private DocumentContext requestContext;
    private Object response;                   // io.restassured.response.Response
    private DocumentContext responseContext;
    private String authToken;
    private final Map<String, Object> testDataMap;
    private String requestLog;
    private String responseLog;

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private Object driver;                     // org.openqa.selenium.WebDriver
    private String currentPage;
    private final Map<String, String> uiTestData;

    // -------------------------------------------------------------------------
    // Shared state
    // -------------------------------------------------------------------------

    private final Map<String, Object> scenarioVars;
    private final Map<String, String> headers;
    private String scenarioName;
    private final List<String> scenarioTags;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Default constructor used by PicoContainer.
     * Initialises all collection fields to empty mutable collections.
     */
    public TestContext() {
        this.testDataMap = new HashMap<>();
        this.uiTestData = new HashMap<>();
        this.scenarioVars = new HashMap<>();
        this.headers = new HashMap<>();
        this.scenarioTags = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // API getters / setters
    // -------------------------------------------------------------------------

    public DocumentContext getRequestContext() { return requestContext; }
    public void setRequestContext(DocumentContext requestContext) { this.requestContext = requestContext; }

    public Object getResponse() { return response; }
    public void setResponse(Object response) { this.response = response; }

    public DocumentContext getResponseContext() { return responseContext; }
    public void setResponseContext(DocumentContext responseContext) { this.responseContext = responseContext; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public Map<String, Object> getTestDataMap() { return testDataMap; }

    public String getRequestLog() { return requestLog; }
    public void setRequestLog(String requestLog) { this.requestLog = requestLog; }

    public String getResponseLog() { return responseLog; }
    public void setResponseLog(String responseLog) { this.responseLog = responseLog; }

    // -------------------------------------------------------------------------
    // UI getters / setters
    // -------------------------------------------------------------------------

    public Object getDriver() { return driver; }
    public void setDriver(Object driver) { this.driver = driver; }

    public String getCurrentPage() { return currentPage; }
    public void setCurrentPage(String currentPage) { this.currentPage = currentPage; }

    public Map<String, String> getUiTestData() { return uiTestData; }

    // -------------------------------------------------------------------------
    // Shared getters / setters
    // -------------------------------------------------------------------------

    public Map<String, Object> getScenarioVars() { return scenarioVars; }

    public Map<String, String> getHeaders() { return headers; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public List<String> getScenarioTags() { return scenarioTags; }
}
