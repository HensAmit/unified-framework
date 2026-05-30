package com.framework.api.service;

import com.framework.api.auth.AuthManager;
import com.framework.common.config.AppConfig;
import com.framework.common.config.ConfigManager;
import com.framework.common.context.TestContext;
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
 * Phase 3 API client. GET only.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Build a {@link RequestSpecification} rooted at {@link AppConfig#apiBaseUrl()}.</li>
 *   <li>Attach the bearer token resolved by {@link AuthManager}.</li>
 *   <li>Attach any headers staged in {@link TestContext#getHeaders()}.</li>
 *   <li>Execute the call and stash the response, request log, and response log
 *       into {@link TestContext} for downstream steps and reporting.</li>
 * </ul>
 *
 * <p>Test code never imports RestAssured directly — all HTTP traffic flows
 * through this class. Phase 4 will extend this with POST/PUT/PATCH/DELETE and
 * with payload-body support; Phase 3 keeps the surface intentionally minimal
 * so wiring problems are easy to diagnose.
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

    /**
     * Issues a GET against {@code endpoint} (relative to the configured base URL)
     * and stores the response into {@link TestContext}.
     *
     * @return the same {@link Response} stored in the context, for callers that
     *         want to assert immediately
     */
    public Response get(String endpoint) {
        String token = authManager.getToken();
        ctx.setAuthToken(token);

        RequestSpecification spec = new RequestSpecBuilder()
                .setBaseUri(config.apiBaseUrl())
                .addHeader("Authorization", "Bearer " + token)
                .addHeaders(ctx.getHeaders())
                .build()
                .filter(new ContextCapturingFilter(ctx));

        log.info("GET {}{}", config.apiBaseUrl(), endpoint);
        Response response = RestAssured.given().spec(spec).when().get(endpoint);
        ctx.setResponse(response);
        return response;
    }

    /**
     * RestAssured filter that captures the full request and response as
     * human-readable strings into the {@link TestContext}.
     *
     * <p>This is what lets Phase 4's hooks attach request/response detail to
     * the Extent report when a scenario fails. The strings are also useful in
     * console logs when running locally.
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
            // Capture request before sending
            String reqLog = "Method  : " + requestSpec.getMethod() + "\n"
                    + "URI     : " + requestSpec.getURI() + "\n"
                    + "Headers : " + requestSpec.getHeaders();
            ctx.setRequestLog(reqLog);

            Response response = filterContext.next(requestSpec, responseSpec);

            // Capture response after receiving
            String respLog = "Status  : " + response.getStatusCode() + "\n"
                    + "Headers : " + response.getHeaders() + "\n"
                    + "Body    : " + response.getBody().asPrettyString();
            ctx.setResponseLog(respLog);

            return response;
        }
    }
}
