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
 * <p>Supports GET/POST/PUT/PATCH/DELETE with optional JSON bodies. Every verb:
 * <ol>
 *   <li>resolves the auth token and attaches it as a Bearer header,</li>
 *   <li>attaches any headers staged on {@link TestContext#getHeaders()},</li>
 *   <li>attaches a body (for write verbs) from the passed JSON string,</li>
 *   <li>executes via RestAssured,</li>
 *   <li>stores the {@link Response} and a parsed response DocumentContext into
 *       the context, and appends the captured request/response to the context's
 *       interaction list via a filter.</li>
 * </ol>
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
     * Captures each request/response pair and APPENDS it to the context's
     * interaction list, so a multi-call scenario keeps every interaction (not
     * just the last). The failure report can then show all of them.
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

            Response response = filterContext.next(requestSpec, responseSpec);

            String respLog = "Status  : " + response.getStatusCode() + "\n"
                    + "Time    : " + response.getTime() + " ms\n"
                    + "Headers : " + response.getHeaders() + "\n"
                    + "Body    : " + response.getBody().asPrettyString();

            ctx.addHttpInteraction(reqLog, respLog);
            return response;
        }
    }
}
