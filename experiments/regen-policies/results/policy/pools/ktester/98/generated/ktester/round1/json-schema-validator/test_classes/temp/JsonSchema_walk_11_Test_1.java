package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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

    /**
     * Data provider for the “Normal operation – validation enabled” case.
     */
    static Stream<Arguments> normalEnabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = (schema, messages, ctx, vc) -> "OK";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for the “Normal operation – validation disabled” case.
     */
    static Stream<Arguments> normalDisabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", 30);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(null, node, format, false, null, 0));
    }

    /**
     * Data provider for “Null ExecutionContext – should be created internally”.
     */
    static Stream<Arguments> nullExecCtxProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("id", 1);
        OutputFormat<Boolean> format = (schema, messages, ctx, vc) -> true;
        return Stream.of(Arguments.of(null, node, format, true, null, true));
    }

    /**
     * Data provider for “ExecutionCustomizer is invoked”.
     */
    static Stream<Arguments> customizerInvokedProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<String> format = (schema, messages, ctx, vc) -> messages.isEmpty() ? "OK" : "FAIL";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for “Validation failure – messages present”.
     */
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

    /**
     * Data provider for “Array node validation – type mismatch”.
     */
    static Stream<Arguments> arrayMismatchProvider() {
        JsonNode node = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(ARRAY_STRING_SCHEMA, node, format, true, null, 1));
    }

    /**
     * Data provider for “Empty schema – no validators, validation enabled”.
     */
    static Stream<Arguments> emptySchemaProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(EMPTY_SCHEMA, node, format, true, null, 0));
    }

    /**
     * Data provider for “ExecutionContext pre‑existing attributes preserved”.
     */
    static Stream<Arguments> preExistingAttributesProvider() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setAttribute("existing", Boolean.TRUE);
        JsonNode node = JsonNodeFactory.instance.objectNode().put("valid", true);
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean existing = (Boolean) ec.getAttribute("existing");
            Boolean added = (Boolean) ec.getAttribute("new");
            return Boolean.TRUE.equals(existing) && Boolean.TRUE.equals(added);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", Boolean.TRUE);
        return Stream.of(Arguments.of(ctx, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Null ExecutionContext with non‑null customizer”.
     */
    static Stream<Arguments> nullExecCtxWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean custom = (Boolean) ec.getAttribute("custom");
            return Boolean.TRUE.equals(custom);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Validation disabled with customizer invoked”.
     */
    static Stream<Arguments> disabledWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("any", "value");
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean visited = (Boolean) ec.getAttribute("visited");
            return Boolean.TRUE.equals(visited);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, false, customizer, true));
    }

    /**
     * Data provider for “OutputFormat returns null”.
     */
    static Stream<Arguments> outputFormatReturnsNullProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Object> format = (schema, messages, ec, vc) -> null;
        return Stream.of(Arguments.of(null, node, format, true, null, null));
    }

    /* ----------------------------------------------------------------------
     * Helper that runs a walk with the supplied parameters.
     * ---------------------------------------------------------------------- */
    private static <T> T walk(JsonSchema schema, ExecutionContext execCtx, JsonNode node, OutputFormat<T> format, boolean validate, Consumer<ExecutionContext> customizer) {
        JsonSchema effective = (schema != null) ? schema : SCHEMA;
        return effective.walk(execCtx, node, format, validate, customizer);
    }

    /**
     * When {@code validate} is {@code true} the walk method must perform validation and
     * return the set of {@link ValidationMessage}s produced by the schema.
     */
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
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()) && "$.name".equals(m.getPath()));
        assertTrue(typeErrorPresent, "Expected a type‑error message for $.name");
    }

    /**
     * When {@code validate} is {@code false} the walk method must skip validation and
     * therefore return an empty set of messages.
     */
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
    void testNormalOperationEnabled(ExecutionContext execCtx, JsonNode node, OutputFormat<String> format, boolean validate, Consumer<ExecutionContext> customizer, String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation disabled")
    @MethodSource("normalDisabledProvider")
    void testNormalOperationDisabled(ExecutionContext execCtx, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext – internal creation")
    @MethodSource("nullExecCtxProvider")
    void testNullExecutionContext(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionCustomizer is invoked")
    @MethodSource("customizerInvokedProvider")
    void testCustomizerInvoked(ExecutionContext execCtx, JsonNode node, OutputFormat<String> format, boolean validate, Consumer<ExecutionContext> customizer, String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation failure – messages present")
    @MethodSource("validationFailureProvider")
    void testValidationFailure(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array node validation – type mismatch")
    @MethodSource("arrayMismatchProvider")
    void testArrayTypeMismatch(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Empty schema – no validators")
    @MethodSource("emptySchemaProvider")
    void testEmptySchema(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionContext pre‑existing attributes preserved")
    @MethodSource("preExistingAttributesProvider")
    void testPreExistingAttributes(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext with non‑null customizer")
    @MethodSource("nullExecCtxWithCustomizerProvider")
    void testNullExecCtxWithCustomizer(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation disabled with customizer invoked")
    @MethodSource("disabledWithCustomizerProvider")
    void testDisabledWithCustomizer(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] OutputFormat returns null")
    @MethodSource("outputFormatReturnsNullProvider")
    void testOutputFormatReturnsNull(ExecutionContext execCtx, JsonNode node, OutputFormat<Object> format, boolean validate, Consumer<ExecutionContext> customizer, Object expected) {
        Object result = walk(null, execCtx, node, format, validate, customizer);
        assertNull(result);
    }

    @Test
    void testNullJsonNodeThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = (s, m, c, v) -> "";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, null, format, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testNullOutputFormatThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, node, null, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCustomizerThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<String> format = (s, m, c, v) -> "unused";
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, c -> {
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
            public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
                throw new RuntimeException("format failure");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, (Consumer<ExecutionContext>) null));
        assertEquals("format failure", thrown.getMessage());
    }
}
