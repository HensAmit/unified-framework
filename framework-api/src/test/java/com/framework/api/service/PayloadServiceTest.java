package com.framework.api.service;

import com.framework.common.context.TestContext;
import com.jayway.jsonpath.DocumentContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableTypeRegistry;
import io.cucumber.datatable.DataTableTypeRegistryTableConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class PayloadServiceTest {

    private TestContext ctx;
    private PayloadService payloadService;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        payloadService = new PayloadService(ctx, new ValueParser(ctx));
    }

    @Test
    @DisplayName("loadPayload reads the template and stores it on the context")
    void loadPayload() {
        DocumentContext doc = payloadService.loadPayload("payloads/playlist/create-playlist.json");
        assertNotNull(doc);
        assertSame(doc, ctx.getRequestContext());
        assertEquals("Default Playlist Name", doc.read("$.name"));
        assertEquals(false, doc.read("$.public"));
    }

    @Test
    @DisplayName("set operation overwrites an existing field")
    void setOperation() {
        payloadService.loadPayload("payloads/playlist/create-playlist.json");
        DataTable table = tableOf(
                Arrays.asList("op", "path", "value", "type"),
                Arrays.asList("set", "$.name", "My Playlist", "string"),
                Arrays.asList("set", "$.public", "true", "boolean"));
        payloadService.update(table);

        DocumentContext doc = ctx.getRequestContext();
        assertEquals("My Playlist", doc.read("$.name"));
        assertEquals(true, doc.read("$.public"));
    }

    @Test
    @DisplayName("set defaults when op column is absent")
    void setIsDefaultOperation() {
        payloadService.loadPayload("payloads/playlist/create-playlist.json");
        DataTable table = tableOf(
                Arrays.asList("path", "value", "type"),
                Arrays.asList("$.description", "Updated desc", "string"));
        payloadService.update(table);

        assertEquals("Updated desc", ctx.getRequestContext().read("$.description"));
    }

    @Test
    @DisplayName("delete operation removes a field")
    void deleteOperation() {
        payloadService.loadPayload("payloads/playlist/create-playlist.json");
        DataTable table = tableOf(
                Arrays.asList("op", "path", "value", "type"),
                Arrays.asList("delete", "$.collaborative", "", ""));
        payloadService.update(table);

        // Reading a deleted path throws PathNotFoundException
        assertThrows(Exception.class, () -> ctx.getRequestContext().read("$.collaborative"));
    }

    @Test
    @DisplayName("placeholder in value is resolved during update")
    void resolvesPlaceholderDuringUpdate() {
        ctx.getScenarioVars().put("desc", "Generated description");
        payloadService.loadPayload("payloads/playlist/create-playlist.json");
        DataTable table = tableOf(
                Arrays.asList("path", "value", "type"),
                Arrays.asList("$.description", "${desc}", "string"));
        payloadService.update(table);

        assertEquals("Generated description", ctx.getRequestContext().read("$.description"));
    }

    @Test
    @DisplayName("update without a loaded payload throws")
    void updateWithoutPayloadThrows() {
        DataTable table = tableOf(
                Arrays.asList("path", "value", "type"),
                Arrays.asList("$.name", "x", "string"));
        assertThrows(IllegalStateException.class, () -> payloadService.update(table));
    }

    /**
     * Builds a Cucumber DataTable from header + data rows.
     *
     * <p>Unlike DataTables created by Cucumber at runtime, a manually-built one
     * needs an explicit {@link DataTableTypeRegistryTableConverter} attached, or
     * {@code asMaps()} throws "DataTable was created without a converter".
     */
    @SafeVarargs
    private static DataTable tableOf(List<String>... rows) {
        DataTableTypeRegistry registry = new DataTableTypeRegistry(Locale.ENGLISH);
        DataTableTypeRegistryTableConverter converter =
                new DataTableTypeRegistryTableConverter(registry);
        return DataTable.create(Arrays.asList(rows), converter);
    }
}
