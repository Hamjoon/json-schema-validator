package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_6_Test {

    /**
     * ObjectMapper used to build {@link JsonNode} instances from JSON strings.
     */
    private ObjectMapper mapper;

    /**
     * The JsonSchema instance under test – a simple “object” schema.
     */
    private JsonSchema schema;

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
        // A very small schema that accepts any JSON object.
        String schemaJson = "{\"type\":\"object\"}";
        // Use the default factory (draft‑2020‑12) – no special configuration needed.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created successfully");
    }

    /**
     * Creates a fresh {@link ExecutionContext} for the current {@code schema}.
     */
    private ExecutionContext newExecutionContext() {
        // The public API provides a factory method on JsonSchema.
        return schema.createExecutionContext();
    }

    /**
     * Utility to build a {@link JsonSchema} from a raw JSON schema string.
     */
    private JsonSchema buildSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Verify that {@code walk(...)} returns a successful {@link ValidationResult}
     * when the input node satisfies the schema and that the supplied customizer
     * is executed.
     */
    @Test
    void walk_WithValidNode_InvokesCustomizerAndReturnsValidResult() throws Exception {
        // GIVEN – a valid JSON object node and a fresh execution context
        JsonNode validNode = mapper.readTree("{\"anyProperty\":\"anyValue\"}");
        ExecutionContext executionContext = new ExecutionContext();
        // A Mockito mock to verify that the consumer is called exactly once
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<ExecutionContext> customizer = mock(java.util.function.Consumer.class);
        // WHEN – the walk method is invoked
        ValidationResult result = schema.walk(executionContext, validNode, true, customizer);
        // THEN – the result must be non‑null and indicate success
        assertNotNull(result, "walk should return a ValidationResult");
        // ValidationResult in networknt library provides isValid()
        assertTrue(result.isValid(), "Result should be valid for a conforming node");
        // AND – the customizer must have been invoked with the same ExecutionContext
        verify(customizer, times(1)).accept(executionContext);
    }

    /**
     * Verify that {@code walk(...)} returns an invalid {@link ValidationResult}
     * when the supplied node does not satisfy the schema.
     */
    @Test
    void walk_WithInvalidNode_ReturnsInvalidResult() throws Exception {
        // GIVEN – a JSON node that is NOT an object (schema expects an object)
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode invalidNode = mapper.readTree("\"just a string\"");
        ExecutionContext executionContext = schema.createExecutionContext();
        // No customizer needed for this scenario
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // WHEN
        ValidationResult result = schema.walk(executionContext, invalidNode, true, noop);
        // THEN – the result should be invalid and contain at least one message
        assertNotNull(result, "walk should return a ValidationResult");
        assertFalse(result.isValid(), "Result should be invalid for a non‑object node");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Validation messages should be available");
        assertFalse(messages.isEmpty(), "There should be at least one validation message");
    }

    /* --------------------------------------------------------------
     *  1. walk_ValidateTrue_ValidNode_NoCustomizer
     * -------------------------------------------------------------- */
    @Test
    void walk_ValidateTrue_ValidNode_NoCustomizer() throws Exception {
        // simple schema that requires a string property "name"
        schema = buildSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode validNode = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        ExecutionContext ctx = newExecutionContext();
        ValidationResult result = schema.walk(ctx, validNode, true, null);
        assertNotNull(result);
        assertTrue(result.isValid(), "Result should be valid for a conforming node");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------
     *  2. walk_ValidateTrue_InvalidNode_NoCustomizer
     * -------------------------------------------------------------- */
    @Test
    void walk_ValidateTrue_InvalidNode_NoCustomizer() throws Exception {
        // same schema as above – missing required "name"
        schema = buildSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode invalidNode = mapper.readTree("{\"age\":30}");
        ExecutionContext ctx = newExecutionContext();
        ValidationResult result = schema.walk(ctx, invalidNode, true, null);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result should be invalid when required property is missing");
        Set<ValidationMessage> msgs = result.getMessages();
        assertNotNull(msgs);
        assertFalse(msgs.isEmpty(), "At least one validation message expected");
    }

    /* --------------------------------------------------------------
     *  3. walk_ValidateFalse_NodeIrrelevant_NoCustomizer
     * -------------------------------------------------------------- */
    @Test
    void walk_ValidateFalse_NodeIrrelevant_NoCustomizer() throws Exception {
        // any schema – validation will be skipped
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode anyNode = mapper.readTree("{\"unexpected\":true}");
        ExecutionContext ctx = newExecutionContext();
        ValidationResult result = schema.walk(ctx, anyNode, false, null);
        assertNotNull(result);
        assertTrue(result.isValid(), "Result must be valid when validation flag is false");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------
     *  4. walk_WithCustomizer_ModifiesContext
     * -------------------------------------------------------------- */
    @Test
    void walk_WithCustomizer_ModifiesContext() throws Exception {
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = newExecutionContext();
        Consumer<ExecutionContext> customizer = executionContext -> {
            // The ExecutionContext API provides a mutable custom data map.
            executionContext.putCustomData("customFlag", true);
        };
        ValidationResult result = schema.walk(ctx, node, true, customizer);
        assertNotNull(result);
        assertTrue(result.isValid(), "Result should be valid for a conforming node");
        assertEquals(Boolean.TRUE, ctx.getCustomData().get("customFlag"), "Customizer should have inserted the customFlag entry");
    }

    /* --------------------------------------------------------------
     *  5. walk_NullExecutionContext_ShouldThrowNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_NullExecutionContext_ShouldThrowNullPointerException() throws Exception {
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, true, null), "Passing null ExecutionContext must throw NPE");
    }

    /* --------------------------------------------------------------
     *  6. walk_NullNode_ShouldThrowNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_NullNode_ShouldThrowNullPointerException() {
        schema = buildSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = newExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(ctx, null, true, null), "Passing null JsonNode must throw NPE");
    }

    /* --------------------------------------------------------------
     *  7. walk_WithCustomizer_ThrowsException_ShouldPropagate
     * -------------------------------------------------------------- */
    @Test
    void walk_WithCustomizer_ThrowsException_ShouldPropagate() throws Exception {
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContext ctx = newExecutionContext();
        Consumer<ExecutionContext> badCustomizer = executionContext -> {
            throw new IllegalStateException("customizer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.walk(ctx, node, true, badCustomizer), "Exception from customizer must propagate");
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  8. walk_InvalidRef_ShouldThrowJsonSchemaException
     * -------------------------------------------------------------- */
    @Test
    void walk_InvalidRef_ShouldThrowJsonSchemaException() throws Exception {
        // $ref points to a non‑existent fragment
        schema = buildSchema("{\"$ref\":\"#/nonExisting\"}");
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        ExecutionContext ctx = newExecutionContext();
        // The library may throw JsonSchemaException or IllegalArgumentException.
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, true, null), "Invalid $ref must cause an exception");
    }

    /* --------------------------------------------------------------
     *  9. walk_RecursiveSchema_ExceedsDepth_ShouldThrowStackOverflowError
     * -------------------------------------------------------------- */
    @Test
    @Disabled("Running this test may cause JVM stack overflow; enable only when the implementation caps recursion depth.")
    void walk_RecursiveSchema_ExceedsDepth_ShouldThrowStackOverflowError() throws Exception {
        // Recursive schema without $recursiveAnchor
        String recursiveSchema = """
            {
              "type":"object",
              "properties":{
                "next":{
                  "$ref":"#"
                }
              }
            }
            """;
        schema = buildSchema(recursiveSchema);
        JsonNode node = mapper.readTree("{\"next\":{\"next\":{\"next\":{}}}}");
        ExecutionContext ctx = newExecutionContext();
        // Depending on the library version this may be StackOverflowError or a custom IllegalStateException.
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, true, null), "Recursive schema should eventually exceed allowed recursion depth");
    }

    /* --------------------------------------------------------------
     * 10. walk_CustomizerThrowsNullPointerException_ShouldPropagate
     * -------------------------------------------------------------- */
    @Test
    void walk_CustomizerThrowsNullPointerException_ShouldPropagate() throws Exception {
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContext ctx = newExecutionContext();
        Consumer<ExecutionContext> npeCustomizer = executionContext -> {
            throw new NullPointerException("customizer NPE");
        };
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schema.walk(ctx, node, true, npeCustomizer), "NullPointerException from customizer must propagate");
        assertEquals("customizer NPE", ex.getMessage());
    }

    /* --------------------------------------------------------------
     * 11. walk_CustomizerModifiesImmutableContext_ShouldThrowUnsupportedOperationException
     * -------------------------------------------------------------- */
    @Test
    void walk_CustomizerModifiesImmutableContext_ShouldThrowUnsupportedOperationException() throws Exception {
        schema = buildSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"a\":1}");
        ExecutionContext ctx = newExecutionContext();
        // Make the internal custom data map immutable via reflection.
        // ExecutionContext has a private field `customData` of type Map<String,Object>.
        java.lang.reflect.Field dataField = ExecutionContext.class.getDeclaredField("customData");
        dataField.setAccessible(true);
        Map<String, Object> immutableMap = Collections.unmodifiableMap(Map.of());
        dataField.set(ctx, immutableMap);
        Consumer<ExecutionContext> mutatingCustomizer = executionContext -> {
            // This will trigger UnsupportedOperationException because the map is unmodifiable.
            executionContext.putCustomData("key", "value");
        };
        assertThrows(UnsupportedOperationException.class, () -> schema.walk(ctx, node, true, mutatingCustomizer), "Modifying an immutable ExecutionContext must throw UnsupportedOperationException");
    }
}
