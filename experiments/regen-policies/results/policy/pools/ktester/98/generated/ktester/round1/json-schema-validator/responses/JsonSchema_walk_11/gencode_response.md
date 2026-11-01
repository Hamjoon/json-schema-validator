```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Extended unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer)}.
 *
 * <p>The tests cover normal operation, handling of {@code null} arguments,
 * execution‑customizer behaviour, propagation of exceptions and a few edge
 * cases such as empty schemas and array validation.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static ObjectMapper MAPPER;
    private static JsonSchemaFactory FACTORY;
    private static JsonSchema SCHEMA;               // schema with required string property "name"
    private static JsonSchema EMPTY_SCHEMA;         // "{}"
    private static JsonSchema ARRAY_STRING_SCHEMA; // expects array of strings

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
     * Existing tests (kept unchanged)
     * ---------------------------------------------------------------------- */

    @Test
    void walk_validatesWhenFlagIsTrue() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {};
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, true, customizer);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        boolean typeErrorPresent = result.stream()
                .anyMatch(m -> "type".equals(m.getType()) && "$.name".equals(m.getPath()));
        assertTrue(typeErrorPresent);
    }

    @Test
    void walk_skipsValidationWhenFlagIsFalse() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {};
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, false, customizer);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Parameterised tests derived from the JSON test‑case description
     * ---------------------------------------------------------------------- */

    /** Data provider for the “Normal operation – validation enabled” case. */
    static Stream<Arguments> normalEnabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = new OutputFormat<String>() {
            @Override
            public String apply(ValidationResult r) {
                return "OK";
            }
        };
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /** Data provider for the “Normal operation – validation disabled” case. */
    static Stream<Arguments> normalDisabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", 30);
        OutputFormat<Integer> format = new OutputFormat<Integer>() {
            @Override
            public Integer apply(ValidationResult r) {
                return r.getMessages().size();
            }
        };
        return Stream.of(Arguments.of(null, node, format, false, null, 0));
    }

    /** Data provider for “Null ExecutionContext – should be created internally”. */
    static Stream<Arguments> nullExecCtxProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("id", 1);
        OutputFormat<Boolean> format = r -> true;
        return Stream.of(Arguments.of(null, node, format, true, null, true));
    }

    /** Data provider for “ExecutionCustomizer is invoked”. */
    static Stream<Arguments> customizerInvokedProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<String> format = r -> r.getMessages().isEmpty() ? "OK" : "FAIL";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /** Data provider for “Validation failure – messages present”. */
    static Stream<Arguments> validationFailureProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", "twenty");
        // schema that expects integer for “age”
        JsonSchema schema = FACTORY.getSchema("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "age": { "type": "integer" } }
                }
                """);
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(schema, node, format, true, null, 1));
    }

    /** Data provider for “Array node validation – type mismatch”. */
    static Stream<Arguments> arrayMismatchProvider() {
        JsonNode node = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(ARRAY_STRING_SCHEMA, node, format, true, null, 1));
    }

    /** Data provider for “Empty schema – no validators, validation enabled”. */
    static Stream<Arguments> emptySchemaProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(EMPTY_SCHEMA, node, format, true, null, 0));
    }

    /** Data provider for “ExecutionContext pre‑existing attributes preserved”. */
    static Stream<Arguments> preExistingAttributesProvider() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setAttribute("existing", true);
        JsonNode node = JsonNodeFactory.instance.objectNode().put("valid", true);
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("existing"))
                    && Boolean.TRUE.equals(ec.getAttribute("new"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", true);
        return Stream.of(Arguments.of(ctx, node, format, true, customizer, true));
    }

    /** Data provider for “Null ExecutionContext with non‑null customizer”. */
    static Stream<Arguments> nullExecCtxWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("custom"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, true));
    }

    /** Data provider for “Validation disabled with customizer invoked”. */
    static Stream<Arguments> disabledWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("any", "value");
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("visited"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", true);
        return Stream.of(Arguments.of(null, node, format, false, customizer, true));
    }

    /** Data provider for “OutputFormat returns null”. */
    static Stream<Arguments> outputFormatReturnsNullProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Object> format = r -> null;
        return Stream.of(Arguments.of(null, node, format, true, null, null));
    }

    /* ----------------------------------------------------------------------
     * Helper that runs a walk with the supplied parameters.
     * ---------------------------------------------------------------------- */
    private static <T> T walk(JsonSchema schema,
                              ExecutionContext execCtx,
                              JsonNode node,
                              OutputFormat<T> format,
                              boolean validate,
                              Consumer<ExecutionContext> customizer) {
        // If the caller passed null for the schema we fall back to the default one.
        JsonSchema effectiveSchema = (schema != null) ? schema : SCHEMA;
        return effectiveSchema.walk(execCtx, node, format, validate, customizer);
    }

    /* ----------------------------------------------------------------------
     * Tests using the above providers
     * ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] Normal operation – validation enabled")
    @MethodSource("normalEnabledProvider")
    void testNormalOperationEnabled(ExecutionContext execCtx,
                                    JsonNode node,
                                    OutputFormat<String> format,
                                    boolean validate,
                                    Consumer<ExecutionContext> customizer,
                                    String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation disabled")
    @MethodSource("normalDisabledProvider")
    void testNormalOperationDisabled(ExecutionContext execCtx,
                                     JsonNode node,
                                     OutputFormat<Integer> format,
                                     boolean validate,
                                     Consumer<ExecutionContext> customizer,
                                     Integer expected) {
        Integer result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext – internal creation")
    @MethodSource("nullExecCtxProvider")
    void testNullExecutionContext(ExecutionContext execCtx,
                                  JsonNode node,
                                  OutputFormat<Boolean> format,
                                  boolean validate,
                                  Consumer<ExecutionContext> customizer,
                                  Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionCustomizer is invoked")
    @MethodSource("customizerInvokedProvider")
    void testCustomizerInvoked(ExecutionContext execCtx,
                               JsonNode node,
                               OutputFormat<String> format,
                               boolean validate,
                               Consumer<ExecutionContext> customizer,
                               String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation failure – messages present")
    @MethodSource("validationFailureProvider")
    void testValidationFailure(JsonSchema schema,
                               JsonNode node,
                               OutputFormat<Integer> format,
                               boolean validate,
                               Consumer<ExecutionContext> customizer,
                               Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array node validation – type mismatch")
    @MethodSource("arrayMismatchProvider")
    void testArrayTypeMismatch(JsonSchema schema,
                                JsonNode node,
                                OutputFormat<Integer> format,
                                boolean validate,
                                Consumer<ExecutionContext> customizer,
                                Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Empty schema – no validators")
    @MethodSource("emptySchemaProvider")
    void testEmptySchema(JsonSchema schema,
                         JsonNode node,
                         OutputFormat<Integer> format,
                         boolean validate,
                         Consumer<ExecutionContext> customizer,
                         Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionContext pre‑existing attributes preserved")
    @MethodSource("preExistingAttributesProvider")
    void testPreExistingAttributes(ExecutionContext execCtx,
                                   JsonNode node,
                                   OutputFormat<Boolean> format,
                                   boolean validate,
                                   Consumer<ExecutionContext> customizer,
                                   Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext with non‑null customizer")
    @MethodSource("nullExecCtxWithCustomizerProvider")
    void testNullExecCtxWithCustomizer(ExecutionContext execCtx,
                                      JsonNode node,
                                      OutputFormat<Boolean> format,
                                      boolean validate,
                                      Consumer<ExecutionContext> customizer,
                                      Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation disabled with customizer invoked")
    @MethodSource("disabledWithCustomizerProvider")
    void testDisabledWithCustomizer(ExecutionContext execCtx,
                                    JsonNode node,
                                    OutputFormat<Boolean> format,
                                    boolean validate,
                                    Consumer<ExecutionContext> customizer,
                                    Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] OutputFormat returns null")
    @MethodSource("outputFormatReturnsNullProvider")
    void testOutputFormatReturnsNull(ExecutionContext execCtx,
                                    JsonNode node,
                                    OutputFormat<Object> format,
                                    boolean validate,
                                    Consumer<ExecutionContext> customizer,
                                    Object expected) {
        Object result = walk(null, execCtx, node, format, validate, customizer);
        assertNull(result);
    }

    /* ----------------------------------------------------------------------
     * Tests that verify exception propagation
     * ---------------------------------------------------------------------- */

    @Test
    void testNullJsonNodeThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = r -> "";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SCHEMA.walk(ctx, null, format, true, null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testNullOutputFormatThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SCHEMA.walk(ctx, node, null, true, null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCustomizerThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<String> format = r -> "unused";
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SCHEMA.walk(ctx, node, format, true,
                        c -> { throw new RuntimeException("customizer failure"); }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    @Test
    void testOutputFormatThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = new OutputFormat<String>() {
            @Override
            public String apply(ValidationResult r) {
                throw new RuntimeException("format failure");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> SCHEMA.walk(ctx, node, format, true, null));
        assertEquals("format failure", thrown.getMessage());
    }
}
```