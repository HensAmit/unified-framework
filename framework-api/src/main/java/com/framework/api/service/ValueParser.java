package com.framework.api.service;

import com.framework.common.context.TestContext;
import com.framework.common.service.PlaceholderResolver;
import com.framework.common.utils.JsonUtils;

/**
 * Converts a raw DataTable cell value into a correctly-typed Java object,
 * resolving {@code ${placeholders}} along the way.
 *
 * <p>DataTable rows carry a {@code type} column telling us how to interpret the
 * {@code value} column:
 * <ul>
 *   <li>{@code string}  → resolved String</li>
 *   <li>{@code number}  → Integer if it has no decimal point, otherwise Double</li>
 *   <li>{@code boolean} → Boolean</li>
 *   <li>{@code null}    → literal {@code null}</li>
 *   <li>{@code json}    → parsed JSON fragment (Map or List), so it embeds as a
 *                          structure rather than a quoted string</li>
 * </ul>
 *
 * <p>Placeholders are resolved <em>before</em> casting. So a row like
 * {@code | $.id | ${savedId} | string |} first turns {@code ${savedId}} into the
 * stored value, then treats the result as a String.
 *
 * <p>The {@code json} type is what lets a DataTable inject a nested object:
 * {@code | $.address | {"city":"NY"} | json |} parses the fragment into a Map so
 * JsonPath writes a real object, not the literal text {@code "{\"city\":\"NY\"}"}.
 */
public class ValueParser {

    private final PlaceholderResolver resolver;

    public ValueParser(TestContext ctx) {
        this.resolver = new PlaceholderResolver(ctx);
    }

    /**
     * Parses {@code rawValue} according to {@code type}, after resolving any
     * {@code ${placeholders}}.
     *
     * @param rawValue the value column from a DataTable row (may contain placeholders)
     * @param type     one of: string, number, boolean, null, json (case-insensitive)
     * @return the typed value, suitable for writing into a JsonPath document
     */
    public Object parse(String rawValue, String type) {
        String normalisedType = (type == null) ? "string" : type.trim().toLowerCase();

        // 'null' ignores the value entirely.
        if ("null".equals(normalisedType)) {
            return null;
        }

        String resolved = resolver.resolve(rawValue);

        return switch (normalisedType) {
            case "string"  -> resolved;
            case "number"  -> parseNumber(resolved);
            case "boolean" -> Boolean.parseBoolean(resolved);
            case "json"    -> JsonUtils.parse(resolved).json();   // returns Map/List structure
            default -> throw new IllegalArgumentException(
                    "Unknown value type '" + type + "'. Supported: string, number, boolean, null, json.");
        };
    }

    /**
     * Resolves placeholders in a value without type casting — used where the
     * result is always a String (e.g. assertion expected values, URL params).
     */
    public String resolveString(String rawValue) {
        return resolver.resolve(rawValue);
    }

    private static Number parseNumber(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot parse null as number");
        }
        if (value.contains(".")) {
            return Double.parseDouble(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // value too large for int — fall back to long
            return Long.parseLong(value);
        }
    }
}
