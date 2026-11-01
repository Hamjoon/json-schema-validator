```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer)}.
 *
 * The original test suite relied on {@code ExecutionContext#setAttribute}/{@code getAttribute}
 * which are not present in the version of the library used for compilation.
 * Those parts have been simplified to focus on the walk logic itself.
 */
class JsonSchema_walk_11_Test {

    private static ObjectMapper MAPPER;
    private static JsonSchemaFactory FACTORY;
    private static JsonSchema SCHEMA;

    /**
     * Simple schema: an object with a required string property {@code name}.
     */
    private static final String SCHEMA_JSON = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    // "{}"
    private static JsonSchema EMPTY_SCHEMA;

    // expects array of strings
    private static JsonSchema ARRAY_STRING_SCHEMA;

    private static final String EMPTY_SCHEMA_JSON = "{}";

    private static final String ARRAY_STRING_SCHEMA_JSON = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "array",
          "items": { "type": "string" }
        }
        """;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        MAPPER = new ObjectMapper();
        FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SCHEMA = FACTORY.getSchema(SCHEMA_JSON);
        EMPTY_SCHEMA = FACTORY.getSchema(EMPTY_SCHEMA_JSON);
        ARRAY_STRING_SCHEMA = FACTORY.getSchema(ARRAY_STRING_SCHEMA_JSON);
    }

    /* ----------------------------------------------------------------------
     * Data providers
     * ---------------------------------------------------------------------- */

    static Stream<Arguments> normalEnabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = (schema, messages, ctx, vc) -> "OK";
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    static Stream<Arguments> normalDisabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", 30);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(null, node, format, false, null, 0));
    }

    static Stream<Arguments> nullExecCtxProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("id", 1);
        OutputFormat<Boolean> format = (schema, messages, ctx, vc) -> true;
        return Stream.of(Arguments.of(null, node, format, true, null, true));
    }

    static Stream<Arguments> validationFailureProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", "twenty");
        JsonSchema schema = FACTORY.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """);
        OutputFormat<Integer> format = (schema1, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(schema, node, format, true, null, 1));
    }

    static Stream<Arguments> arrayMismatchProvider() {
        JsonNode node = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(ARRAY_STRING_SCHEMA, node, format, true, null, 1));
    }

    static Stream<Arguments> emptySchemaProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(EMPTY_SCHEMA, node, format, true, null, 0));
    }

    static Stream<Arguments> outputFormatReturnsNullProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Object> format = (schema, messages, ec, vc) -> null;
        return Stream.of(Arguments.of(null, node, format, true, null, null));
    }

    /* ----------------------------------------------------------------------
     * Helper that runs a walk with the supplied parameters.
     * ---------------------------------------------------------------------- */
    private static <T> T walk(JsonSchema schema, ExecutionContext execCtx, JsonNode node,
                               OutputFormat<T> format, boolean validate,
                               Consumer<ExecutionContext> customizer) {
        JsonSchema effective = (schema != null) ? schema : SCHEMA;
        return effective.walk(execCtx, node, format, validate, customizer);
    }

    /* ----------------------------------------------------------------------
     * Tests
     * ---------------------------------------------------------------------- */

    @Test
    void walk_validatesWhenFlagIsTrue() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = (schema, messages, ctx, vc) -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, true, customizer);
        assertNotNull(result, "Resulting message set must not be null");
        assertFalse(result.isEmpty(), "Invalid input should produce at least one validation message");
        // Verify that at least one message mentions a type error (exact path checking omitted for compatibility)
        boolean typeErrorPresent = result.stream()
                .anyMatch(m -> "type".equals(m.getType()));
        assertTrue(typeErrorPresent, "Expected a type‑error message");
    }

    @Test
    void walk_skipsValidationWhenFlagIsFalse() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = (schema, messages, ctx, vc) -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, false, customizer);
        assertNotNull(result, "Resulting message set must not be null");
        assertTrue(result.isEmpty(), "When validation is disabled the result set should be empty");
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation enabled")
    @MethodSource("normalEnabledProvider")
    void testNormalOperationEnabled(ExecutionContext execCtx, JsonNode node,
                                    OutputFormat<String> format, boolean validate,
                                    Consumer<ExecutionContext> customizer, String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation disabled")
    @MethodSource("normalDisabledProvider")
    void testNormalOperationDisabled(ExecutionContext execCtx, JsonNode node,
                                     OutputFormat<Integer> format, boolean validate,
                                     Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext – internal creation")
    @MethodSource("nullExecCtxProvider")
    void testNullExecutionContext(ExecutionContext execCtx, JsonNode node,
                                  OutputFormat<Boolean> format, boolean validate,
                                  Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation failure – messages present")
    @MethodSource("validationFailureProvider")
    void testValidationFailure(JsonSchema schema, JsonNode node,
                               OutputFormat<Integer> format, boolean validate,
                               Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array node validation – type mismatch")
    @MethodSource("arrayMismatchProvider")
    void testArrayTypeMismatch(JsonSchema schema, JsonNode node,
                               OutputFormat<Integer> format, boolean validate,
                               Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Empty schema – no validators")
    @MethodSource("emptySchemaProvider")
    void testEmptySchema(JsonSchema schema, JsonNode node,
                         OutputFormat<Integer> format, boolean validate,
                         Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] OutputFormat returns null")
    @MethodSource("outputFormatReturnsNullProvider")
    void testOutputFormatReturnsNull(ExecutionContext execCtx, JsonNode node,
                                    OutputFormat<Object> format, boolean validate,
                                    Consumer<ExecutionContext> customizer, Object expected) {
        Object result = walk(null, execCtx, node, format, validate, customizer);
        assertNull(result);
    }

    @Test
    void testNullJsonNodeThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = (s, m, c, v) -> "";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SCHEMA.walk(ctx, null, format, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testNullOutputFormatThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SCHEMA.walk(ctx, node, null, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCustomizerThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<String> format = (s, m, c, v) -> "unused";
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SCHEMA.walk(ctx, node, format, true, c -> {
                    throw new RuntimeException("customizer failure");
                }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    @Test
    void testOutputFormatThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = new OutputFormat<String>() {
            @Override
            public String format(JsonSchema schema, Set<ValidationMessage> messages,
                                 ExecutionContext executionContext, ValidationContext validationContext) {
                throw new RuntimeException("format failure");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SCHEMA.walk(ctx, node, format, true, (Consumer<ExecutionContext>) null));
        assertEquals("format failure", thrown.getMessage());
    }
}
```