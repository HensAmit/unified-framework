package com.framework.api.service;

import com.framework.api.auth.AuthManager;
import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.context.TestContext;
import com.framework.common.utils.JsonUtils;
import com.framework.common.utils.LogUtils;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.Logger;

/**
 * The framework's HTTP client. All API traffic flows through this class — test
 * code never imports RestAssured directly.
 *
 * <p>Phase 4 adds POST/PUT/PATCH/DELETE and request-body support to the GET-only
 * Phase 3 version. Every verb:
 * <ol>
 *   <li>resolves the auth token and attaches it as a Bearer header,</li>
 *   <li>attaches any headers staged on {@link TestContext#getHeaders()},</li>
 *   <li>attaches a body (for write verbs) from the passed JSON string,</li>
 *   <li>executes via RestAssured,</li>
 *   <li>stores the {@link Response} and a parsed response DocumentContext into
 *       the context, plus captures request/response logs via a filter.</li>
 * </ol>
 *
 * <p>The parsed response context uses Jayway JsonPath ({@code $.field} syntax),
 * matching the payload syntax, so assertions read responses with the same path
 * style used to build requests.
 */
public class ApiService {

    private static final Logger log = LogUtils.getLogger(ApiService.class);

    private final TestContext ctx;
    private final AuthManager authManager;
    private final AppConfig config;

    public ApiService(TestContext ctx, AuthManager authManager) {
        this.ctx = ctx;
        this.authManager = authManager;
        this.config = ConfigManager.get();
    }

    // -------------------------------------------------------------------------
    // HTTP verbs
    // -------------------------------------------------------------------------

    public Response get(String endpoint) {
        log.info("GET {}{}", config.apiBaseUrl(), endpoint);
        return execute("GET", endpoint, null);
    }

    public Response post(String endpoint, String body) {
        log.info("POST {}{}", config.apiBaseUrl(), endpoint);
        return execute("POST", endpoint, body);
    }

    public Response put(String endpoint, String body) {
        log.info("PUT {}{}", config.apiBaseUrl(), endpoint);
        return execute("PUT", endpoint, body);
    }

    public Response patch(String endpoint, String body) {
        log.info("PATCH {}{}", config.apiBaseUrl(), endpoint);
        return execute("PATCH", endpoint, body);
    }

    public Response delete(String endpoint) {
        log.info("DELETE {}{}", config.apiBaseUrl(), endpoint);
        return execute("DELETE", endpoint, null);
    }

    // -------------------------------------------------------------------------
    // Core execution
    // -------------------------------------------------------------------------

    private Response execute(String method, String endpoint, String body) {
        String token = authManager.getToken();
        ctx.setAuthToken(token);

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(config.apiBaseUrl())
                .addHeader("Authorization", "Bearer " + token)
                .addHeaders(ctx.getHeaders());

        if (body != null) {
            builder.setContentType("application/json").setBody(body);
        }

        RequestSpecification spec = builder.build()
                .filter(new ContextCapturingFilter(ctx));

        RequestSpecification request = RestAssured.given().spec(spec);
        Response response = switch (method) {
            case "GET"    -> request.when().get(endpoint);
            case "POST"   -> request.when().post(endpoint);
            case "PUT"    -> request.when().put(endpoint);
            case "PATCH"  -> request.when().patch(endpoint);
            case "DELETE" -> request.when().delete(endpoint);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        ctx.setResponse(response);
        parseResponseBody(response);
        return response;
    }

    /**
     * Parses the response body into a Jayway DocumentContext and stores it on
     * the context, so assertions can read fields with {@code $.path} syntax.
     * Bodies that aren't valid JSON (empty 204s, plain-text errors) are
     * tolerated — the response context is simply left null.
     */
    private void parseResponseBody(Response response) {
        String bodyString = response.getBody() == null ? null : response.getBody().asString();
        if (bodyString == null || bodyString.isBlank()) {
            ctx.setResponseContext(null);
            return;
        }
        try {
            ctx.setResponseContext(JsonUtils.parse(bodyString));
        } catch (Exception e) {
            log.debug("Response body is not valid JSON; skipping JsonPath parse");
            ctx.setResponseContext(null);
        }
    }

    /**
     * Captures the fully-resolved request and the response as readable strings
     * into the {@link TestContext}, for attachment to the Extent report on failure.
     */
    private static final class ContextCapturingFilter implements Filter {
        private final TestContext ctx;

        ContextCapturingFilter(TestContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext filterContext) {
            String reqLog = "Method  : " + requestSpec.getMethod() + "\n"
                    + "URI     : " + requestSpec.getURI() + "\n"
                    + "Headers : " + requestSpec.getHeaders() + "\n"
                    + "Body    : " + (requestSpec.getBody() == null ? "<none>" : requestSpec.getBody());
            ctx.setRequestLog(reqLog);

            Response response = filterContext.next(requestSpec, responseSpec);

            String respLog = "Status  : " + response.getStatusCode() + "\n"
                    + "Time    : " + response.getTime() + " ms\n"
                    + "Headers : " + response.getHeaders() + "\n"
                    + "Body    : " + response.getBody().asPrettyString();
            ctx.setResponseLog(respLog);

            return response;
        }
    }
}
