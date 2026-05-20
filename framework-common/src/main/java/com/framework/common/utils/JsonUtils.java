package com.framework.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Small JSON convenience helpers.
 *
 * <p>Intentionally minimal in Phase 2; expanded by {@code PayloadService} in
 * Phase 4 once payload templates and DataTable-driven mutation arrive.
 *
 * <p>Two libraries are in play:
 * <ul>
 *   <li>Jackson for serialise/pretty-print</li>
 *   <li>Jayway JsonPath for the {@link DocumentContext} type used throughout
 *       the framework as our parsed-JSON representation</li>
 * </ul>
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
        // utility — no instances
    }

    /**
     * Parses a JSON string into a {@link DocumentContext} for JsonPath access.
     *
     * @throws IllegalArgumentException if the string is not valid JSON
     */
    public static DocumentContext parse(String json) {
        if (json == null) {
            throw new IllegalArgumentException("Cannot parse null JSON");
        }
        try {
            return JsonPath.parse(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Loads and parses a JSON file from the classpath into a {@link DocumentContext}.
     * Used in Phase 4 for payload templates.
     *
     * @param classpathPath path relative to the classpath root, e.g. {@code payloads/playlist/create.json}
     * @throws IllegalArgumentException if the resource cannot be found
     */
    public static DocumentContext parseFromClasspath(String classpathPath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + classpathPath);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(json);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed reading " + classpathPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns a pretty-printed JSON string for any object Jackson can serialise.
     * Used for log readability — not for wire transmission.
     */
    public static String prettyPrint(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);     // never let logging crash the test
        }
    }
}
