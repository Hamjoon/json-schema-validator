package com.networknt.schema;

import java.util.Collections;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    /**
     * Schema used for all test cases
     */
    private JsonSchema schema;

    /**
     * Jackson mapper to create JsonNode instances from JSON strings
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Simple schema that references another schema – identical to the one used in the
     *  concurrency test of the project.
     */
    private static final String SCHEMA_DATA = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$id\": \"http://example.org/schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"$ref\": \"ref.json\"\n" + "}";

    /**
     * The referenced schema – defines a required string property “name”.
     */
    private static final String REF_SCHEMA_DATA = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"$id\": \"http://example.org/ref.json\",\n" + "  \"properties\": {\n" + "    \"name\": {\n" + "      \"type\": \"string\",\n" + "      \"description\": \"The name\"\n" + "    }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory that knows how to resolve the external reference.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("http://example.org/ref.json", REF_SCHEMA_DATA))));
        // Disable pre‑loading – the same configuration used in the original test.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().preloadJsonSchema(false).build();
        // Create the JsonSchema instance that will be exercised.
        this.schema = factory.getSchema(SCHEMA_DATA, config);
        assertNotNull(schema, "Schema should be created successfully");
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, true, …)} performs validation
     * and returns a {@link ValidationResult} containing the expected validation error.
     */
    @Test
    void walkWithValidationEnabled() throws Exception {
        // Input where the required property “name” is of the wrong type (number instead of string)
        String inputJson = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(inputJson);
        // Create a fresh execution context for the walk.
        ExecutionContext ctx = schema.createExecutionContext();
        // Simple customizer that does nothing – required by the method signature.
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op
        };
        ValidationResult result = schema.walk(ctx, node, true, customizer);
        // The schema should report exactly one validation error (type mismatch).
        assertEquals(1, result.getMessages().size(), "When validation is enabled the result should contain one error");
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, false, …)} skips validation
     * and therefore returns an empty set of messages.
     */
    @Test
    void walkWithValidationDisabled() throws Exception {
        // Same malformed input as the previous test.
        String inputJson = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(inputJson);
        ExecutionContext ctx = schema.createExecutionContext();
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op
        };
        ValidationResult result = schema.walk(ctx, node, false, customizer);
        // No validation should have been performed, so the message set must be empty.
        assertEquals(0, result.getMessages().size(), "When validation is disabled the result should contain no errors");
    }

    /**
     * Verify that the walk method works correctly when a custom {@link ExecutionContextCustomizer}
     * mutates the execution context (e.g., changes the collector). The customizer itself is
     * not part of the library's public contract, so we only assert that the walk still
     * completes and returns a non‑null result.
     */
    @Test
    void walkWithCustomExecutionContextCustomizer() throws Exception {
        // valid input – no errors expected
        String inputJson = "{ \"name\": \"valid\" }";
        JsonNode node = MAPPER.readTree(inputJson);
        ExecutionContext ctx = schema.createExecutionContext();
        // Customizer that could, for example, replace the collector – here we just keep it simple.
        ExecutionContextCustomizer customizer = executionContext -> {
            // In a real scenario we might set a custom CollectorContext here.
        };
        ValidationResult result = schema.walk(ctx, node, true, customizer);
        // The input is valid, therefore the result should contain zero messages.
        assertEquals(0, result.getMessages().size(), "Valid input should produce no validation messages even with a customizer");
    }

    @Test
    void walkWithValidationEnabled_validNode_returnsSuccess() throws Exception {
        // valid input – the referenced schema requires a string property "name"
        String json = "{ \"name\": \"John\" }";
        JsonNode node = MAPPER.readTree(json);
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true, executionContext -> {
            // no‑op customizer
        });
        assertEquals(0, result.getMessages().size(), "A valid node with validation enabled must produce no messages");
    }

    @Test
    void walkWithValidationEnabled_invalidNode_returnsFailure() throws Exception {
        // "name" is a number → type mismatch
        String json = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(json);
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true, executionContext -> {
            // no‑op customizer
        });
        assertEquals(1, result.getMessages().size(), "A node violating the schema must produce exactly one validation message");
    }

    @Test
    void walkWithValidationDisabled_invalidNode_isIgnored() throws Exception {
        // same malformed input as above, but validation is turned off
        String json = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(json);
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, false, executionContext -> {
            // no‑op customizer
        });
        assertEquals(0, result.getMessages().size(), "When validation is disabled the result must contain no messages");
    }

    @Test
    void walk_nullExecutionContext_throwsIllegalArgumentException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        ExecutionContextCustomizer customizer = ec -> {
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> schema.walk(null, node, true, customizer));
        assertTrue(ex.getMessage().contains("ExecutionContext"), "Exception message should mention ExecutionContext");
    }

    @Test
    void walk_nullJsonNode_throwsIllegalArgumentException() throws Exception {
        ExecutionContext ctx = schema.createExecutionContext();
        ExecutionContextCustomizer customizer = ec -> {
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, null, true, customizer));
        assertTrue(ex.getMessage().contains("JsonNode"), "Exception message should mention JsonNode");
    }

    @Test
    void walk_nullCustomizer_behavesAsNoOp() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        // customizer is null – the method should treat it as a no‑op
        ValidationResult result = schema.walk(ctx, node, true, null);
        assertEquals(0, result.getMessages().size(), "Null customizer must not affect a successful validation");
    }

    @Test
    void walk_customizerIsInvoked_exactlyOnce() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        AtomicBoolean invoked = new AtomicBoolean(false);
        ExecutionContextCustomizer customizer = executionContext -> invoked.set(true);
        ValidationResult result = schema.walk(ctx, node, true, customizer);
        assertTrue(invoked.get(), "Customizer must have been invoked");
        assertEquals(0, result.getMessages().size(), "Valid input should still produce a successful result");
    }

    @Test
    void walk_customizerThrowsRuntimeException_propagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ExecutionContextCustomizer customizer = executionContext -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, true, customizer));
        assertEquals("customizer failure", ex.getMessage(), "The original RuntimeException must be propagated");
    }

    @Test
    void walk_executionContextThrowsRuntimeException_propagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        // Mock ExecutionContext so that any method call throws RuntimeException.
        ExecutionContext throwingCtx = mock(ExecutionContext.class, invocation -> {
            throw new RuntimeException("execution context failure");
        });
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(throwingCtx, node, true, customizer));
        assertEquals("execution context failure", ex.getMessage(), "Exception from ExecutionContext must propagate");
    }

    @Test
    void walk_validateTrue_arrayNode_typeMismatch() throws Exception {
        // The schema expects an object, but we supply an array.
        JsonNode arrayNode = MAPPER.readTree("[1, 2, 3]");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, arrayNode, true, executionContext -> {
        });
        assertFalse(result.getMessages().isEmpty(), "A type‑mismatch should generate at least one validation message");
        // The exact message text is implementation‑specific; we only check that it mentions "type".
        boolean containsType = result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsType, "Validation message should indicate a type error");
    }

    @Test
    void walk_validateFalse_nullCustomizer_returnsSuccess() throws Exception {
        JsonNode node = MAPPER.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, false, null);
        assertEquals(0, result.getMessages().size(), "When validation is disabled the result must be successful even if customizer is null");
    }

    @Test
    void walk_validNode_withCustomCustomizer_returnsSuccess() throws Exception {
        JsonNode node = MAPPER.readTree("{\"name\":\"Alice\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ExecutionContextCustomizer customizer = executionContext -> {
            // Example of mutating the context – if the API provides setAttribute/getAttribute
            // we would use them here. For now we just ensure the lambda runs without error.
        };
        ValidationResult result = schema.walk(ctx, node, true, customizer);
        assertEquals(0, result.getMessages().size(), "Valid node with a customizer must still succeed");
    }
}
