package com.framework.api.service;

import com.framework.common.context.TestContext;
import com.framework.common.utils.JsonUtils;
import com.framework.common.utils.LogUtils;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Loads JSON payload templates and mutates them via JsonPath.
 *
 * <p>The pattern: a scenario loads a template from {@code /payloads/...}, then
 * applies a DataTable of mutations to customise it before sending. This avoids
 * a proliferation of near-identical POJO classes — one template plus a few
 * targeted edits covers most request shapes.
 *
 * <p>Loaded payloads are parsed into a Jayway {@link DocumentContext} and stored
 * on {@link TestContext#getRequestContext()}, so the {@code ApiService} send
 * methods can pick them up as the request body.
 *
 * <h2>DataTable-driven updates</h2>
 * The {@code update} method accepts a DataTable with columns:
 * {@code | path | value | type |} and an optional {@code | op |} column.
 * Each row applies one operation; {@code op} defaults to {@code set}:
 * <ul>
 *   <li>{@code set}    — set the value at an existing path</li>
 *   <li>{@code add}    — append a value to an array at the path</li>
 *   <li>{@code delete} — remove the field at the path (value/type ignored)</li>
 * </ul>
 *
 * <p>The {@code put} operation (add a new key to an object) is exposed as a
 * programmatic method rather than via DataTable, since it needs a separate key
 * argument that doesn't fit the table cleanly.
 */
public class PayloadService {

    private static final Logger log = LogUtils.getLogger(PayloadService.class);

    private final TestContext ctx;
    private final ValueParser valueParser;

    public PayloadService(TestContext ctx, ValueParser valueParser) {
        this.ctx = ctx;
        this.valueParser = valueParser;
    }

    /**
     * Loads a JSON template from the classpath, parses it, stores it on the
     * context as the current request payload, and returns it.
     *
     * @param classpathPath e.g. {@code payloads/playlist/create-playlist.json}
     */
    public DocumentContext loadPayload(String classpathPath) {
        log.info("Loading payload template: {}", classpathPath);
        DocumentContext doc = JsonUtils.parseFromClasspath(classpathPath);
        ctx.setRequestContext(doc);
        return doc;
    }

    /**
     * Applies a DataTable of mutations to the current request payload.
     * Reads the payload from {@link TestContext#getRequestContext()}.
     */
    public void update(DataTable table) {
        DocumentContext doc = ctx.getRequestContext();
        if (doc == null) {
            throw new IllegalStateException(
                    "No payload loaded — call loadPayload(...) before updating.");
        }
        update(doc, table);
    }

    /**
     * Applies a DataTable of mutations to the given document.
     */
    public void update(DocumentContext doc, DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        for (Map<String, String> row : rows) {
            String op = row.getOrDefault("op", "set").trim().toLowerCase();
            String path = row.get("path");
            String rawValue = row.get("value");
            String type = row.get("type");

            switch (op) {
                case "set" -> {
                    Object value = valueParser.parse(rawValue, type);
                    log.debug("set {} = {}", path, value);
                    doc.set(path, value);
                }
                case "add" -> {
                    Object value = valueParser.parse(rawValue, type);
                    log.debug("add {} <- {}", path, value);
                    doc.add(path, value);
                }
                case "delete" -> {
                    log.debug("delete {}", path);
                    doc.delete(path);
                }
                default -> throw new IllegalArgumentException(
                        "Unknown payload op '" + op + "'. Supported: set, add, delete.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Programmatic operations (used by TestDataFactory and direct callers)
    // -------------------------------------------------------------------------

    public void set(DocumentContext doc, String path, Object value) {
        doc.set(path, value);
    }

    public void put(DocumentContext doc, String path, String key, Object value) {
        doc.put(path, key, value);
    }

    public void add(DocumentContext doc, String path, Object value) {
        doc.add(path, value);
    }

    public void delete(DocumentContext doc, String path) {
        doc.delete(path);
    }
}
