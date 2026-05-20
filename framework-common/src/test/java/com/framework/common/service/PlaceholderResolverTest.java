package com.framework.common.service;

import com.framework.common.context.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderResolverTest {

    private TestContext ctx;
    private PlaceholderResolver resolver;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        resolver = new PlaceholderResolver(ctx);
    }

    @Test
    @DisplayName("returns null when input is null")
    void nullInput() {
        assertNull(resolver.resolve(null));
    }

    @Test
    @DisplayName("returns input unchanged when no placeholders present")
    void noPlaceholders() {
        assertEquals("hello world", resolver.resolve("hello world"));
        assertEquals("", resolver.resolve(""));
        assertEquals("$5.00", resolver.resolve("$5.00"));   // $ alone is not a placeholder
    }

    @Test
    @DisplayName("replaces a single placeholder")
    void singlePlaceholder() {
        ctx.getScenarioVars().put("name", "Alice");
        assertEquals("hello Alice", resolver.resolve("hello ${name}"));
    }

    @Test
    @DisplayName("replaces multiple placeholders in one string")
    void multiplePlaceholders() {
        ctx.getScenarioVars().put("user", "alice");
        ctx.getScenarioVars().put("repo", "framework");
        assertEquals("/users/alice/repos/framework",
                resolver.resolve("/users/${user}/repos/${repo}"));
    }

    @Test
    @DisplayName("handles non-string values via String.valueOf")
    void numericValue() {
        ctx.getScenarioVars().put("id", 42);
        assertEquals("/items/42", resolver.resolve("/items/${id}"));
    }

    @Test
    @DisplayName("handles boolean values")
    void booleanValue() {
        ctx.getScenarioVars().put("flag", true);
        assertEquals("active=true", resolver.resolve("active=${flag}"));
    }

    @Test
    @DisplayName("throws IllegalArgumentException for unresolved placeholder")
    void unresolvedPlaceholder() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("hello ${missing}"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    @DisplayName("error message lists available keys for diagnostic ease")
    void unresolvedPlaceholderListsKeys() {
        ctx.getScenarioVars().put("foo", "1");
        ctx.getScenarioVars().put("bar", "2");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("${nope}"));
        assertTrue(ex.getMessage().contains("foo"));
        assertTrue(ex.getMessage().contains("bar"));
    }

    @Test
    @DisplayName("escapes literal $ and \\ in replacement values")
    void escapesSpecialCharsInReplacement() {
        ctx.getScenarioVars().put("price", "$50");
        ctx.getScenarioVars().put("path", "C:\\Users\\test");
        assertEquals("cost: $50", resolver.resolve("cost: ${price}"));
        assertEquals("at C:\\Users\\test", resolver.resolve("at ${path}"));
    }
}
