package com.framework.api.service;

import com.framework.common.context.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueParserTest {

    private TestContext ctx;
    private ValueParser parser;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        parser = new ValueParser(ctx);
    }

    @Test
    @DisplayName("string type returns the value as-is")
    void stringType() {
        assertEquals("hello", parser.parse("hello", "string"));
    }

    @Test
    @DisplayName("number type without decimal returns Integer")
    void integerType() {
        Object result = parser.parse("30", "number");
        assertInstanceOf(Integer.class, result);
        assertEquals(30, result);
    }

    @Test
    @DisplayName("number type with decimal returns Double")
    void doubleType() {
        Object result = parser.parse("3.14", "number");
        assertInstanceOf(Double.class, result);
        assertEquals(3.14, result);
    }

    @Test
    @DisplayName("boolean type returns Boolean")
    void booleanType() {
        assertEquals(true, parser.parse("true", "boolean"));
        assertEquals(false, parser.parse("false", "boolean"));
    }

    @Test
    @DisplayName("null type returns null regardless of value")
    void nullType() {
        assertNull(parser.parse("anything", "null"));
        assertNull(parser.parse(null, "null"));
    }

    @Test
    @DisplayName("json type parses into a Map structure")
    void jsonObjectType() {
        Object result = parser.parse("{\"city\":\"NY\"}", "json");
        assertInstanceOf(Map.class, result);
        assertEquals("NY", ((Map<?, ?>) result).get("city"));
    }

    @Test
    @DisplayName("placeholders are resolved before casting")
    void resolvesPlaceholders() {
        ctx.getScenarioVars().put("userId", "abc123");
        assertEquals("abc123", parser.parse("${userId}", "string"));
    }

    @Test
    @DisplayName("placeholder inside a number resolves then casts")
    void resolvesPlaceholderThenCastsNumber() {
        ctx.getScenarioVars().put("count", "42");
        Object result = parser.parse("${count}", "number");
        assertEquals(42, result);
    }

    @Test
    @DisplayName("unknown type throws IllegalArgumentException")
    void unknownType() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("x", "widget"));
    }

    @Test
    @DisplayName("type is case-insensitive")
    void caseInsensitiveType() {
        assertEquals(true, parser.parse("true", "BOOLEAN"));
        assertEquals("x", parser.parse("x", "String"));
    }
}
