package com.framework.common.service;

import com.framework.common.context.TestContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${variableName}} placeholders against
 * {@link TestContext#getScenarioVars()}.
 *
 * <p>Used in three places:
 * <ul>
 *   <li>API payloads — values inside DataTable rows are resolved before being
 *       written into the JSON request body</li>
 *   <li>Assertion expected values — same DataTable resolution before
 *       comparison</li>
 *   <li>Step URL parameters — e.g. {@code GET /playlists/${playlistId}}</li>
 * </ul>
 *
 * <p>A scenario typically populates {@code scenarioVars} via the {@code save}
 * assertion type, e.g. {@code | save | $.id | playlistId |} extracts the
 * response field and stores it under the key {@code playlistId}, after which
 * subsequent steps can reference {@code ${playlistId}}.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>A {@code null} input returns {@code null}.</li>
 *   <li>A string with no placeholders is returned unchanged.</li>
 *   <li>Multiple placeholders in one string are all resolved.</li>
 *   <li>An unresolved placeholder (key missing from {@code scenarioVars})
 *       throws {@link IllegalArgumentException}. Failing loud beats silently
 *       substituting empty strings — the latter produces confusing failures
 *       far from the actual cause.</li>
 *   <li>Nested placeholders ({@code ${${inner}}}) are <em>not</em> supported.</li>
 * </ul>
 */
public final class PlaceholderResolver {

    /** Matches {@code ${name}} where {@code name} contains no closing brace. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    private final TestContext ctx;

    /**
     * PicoContainer-injectable constructor. Each scenario gets its own resolver
     * bound to the scenario's {@link TestContext}.
     */
    public PlaceholderResolver(TestContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns {@code value} with all {@code ${var}} placeholders replaced by
     * the corresponding entries in {@link TestContext#getScenarioVars()}.
     *
     * @throws IllegalArgumentException if any placeholder references a key
     *         not present in {@code scenarioVars}.
     */
    public String resolve(String value) {
        if (value == null) {
            return null;
        }

        Matcher m = PLACEHOLDER.matcher(value);
        if (!m.find()) {
            return value;        // fast-path: no placeholders at all
        }

        Map<String, Object> vars = ctx.getScenarioVars();
        StringBuilder out = new StringBuilder();

        // Re-walk from the start; m.find() above only peeked.
        m.reset();
        while (m.find()) {
            String key = m.group(1);
            if (!vars.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Unresolved placeholder ${" + key + "} — no such key in scenarioVars. "
                                + "Available keys: " + vars.keySet());
            }
            Object replacement = vars.get(key);
            // quoteReplacement handles literal $ and \ in the value
            m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(replacement)));
        }
        m.appendTail(out);
        return out.toString();
    }
}
