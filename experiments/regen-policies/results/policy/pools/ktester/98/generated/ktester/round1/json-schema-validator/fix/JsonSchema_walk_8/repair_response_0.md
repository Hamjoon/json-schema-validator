```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.stream.Stream;
import java.util.function.Consumer;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.utils.JsonNodes;

/**
 * Tests for {@link JsonSchema#walk(String, InputFormat, boolean, Consumer)}.
 */
class JsonSchema_walk_8_Test {

    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    private JsonSchema schema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance that will be reused by all tests
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> provideValidJsonWithCustomizer() {
        // Example of how a parameterised test could be added later.
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", true));
    }

    /**
     * Walk the schema with {@code validate = false}. The method should still return a
     * {@link ValidationResult} but without performing any validation – i.e. the
     * result must contain no validation messages.
     */
    @Test
    void walkWithoutValidationReturnsEmptyResult() {
        String validJson = "{\"name\":\"John\"}";
        ValidationResult result = schema.walk(validJson, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) executionContext -> {
                    // The customiser should be invoked even when validation is disabled.
                    assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
                });
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertTrue(messages.isEmpty(), "When validation is disabled there should be no messages");
    }

    /**
     * Walk the schema with {@code validate = true} on an invalid instance.
     * The supplied {@link java.util.function.Consumer} must be invoked and the
     * resulting {@link ValidationResult} must contain the expected validation
     * messages.
     */
    @Test
    void walkWithValidationAndCustomizerDetectsErrors() {
        String invalidJson = "{\"name\":1}";
        AtomicBoolean customiserInvoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) executionContext -> {
                    // verify that the consumer is called and that we receive a non‑null context
                    assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
                    customiserInvoked.set(true);
                });
        assertTrue(customiserInvoked.get(), "The execution customiser should have been invoked");
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        // The schema requires "name" to be a string, therefore we expect at least one message
        assertFalse(messages.isEmpty(), "Invalid input should produce validation messages");
        // Simple sanity check – the message should mention the property "name"
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        assertTrue(mentionsName, "At least one validation message should refer to the 'name' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonWithCustomizerValidationEnabled() {
        String json = "{\"name\":\"John\",\"age\":30}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> {
                    ctx.setAttribute("custom", true);
                    ctxRef.set(ctx);
                });
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isSuccess(), "Result should be valid for correct JSON");
        assertTrue(result.getValidationMessages().isEmpty(), "No validation messages expected");
        assertNotNull(ctxRef.get(), "Consumer must have received a non‑null ExecutionContext");
        assertEquals(true, ctxRef.get().getAttribute("custom"), "Custom attribute should be set by the consumer");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledNoCustomizer() {
        String json = "{\"name\":\"Jane\"}";
        ValidationResult result = schema.walk(json, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isSuccess(), "Result should be valid when validation is disabled");
        assertTrue(result.getValidationMessages().isEmpty(), "No messages should be produced");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkMalformedJsonThrowsException() {
        String malformed = "{\"name\":\"Bob\", \"age\":}";
        // The implementation wraps JsonParseException in a ValidationException,
        // but we assert that *some* exception is thrown.
        assertThrows(RuntimeException.class,
                () -> schema.walk(malformed, InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(null, InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk("{\"name\":\"Alice\"}", null, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkUnsupportedUrlEncodedThrowsException() {
        // %E0%A4%A is an illegal percent‑encoding sequence
        String badEncoded = "%E0%A4%A";
        assertThrows(RuntimeException.class,
                () -> schema.walk(badEncoded, InputFormat.URL_ENCODED, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkConsumerThrowsRuntimeExceptionPropagates() {
        String json = "{\"id\":1}";
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> schema.walk(json, InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) ctx -> {
                            throw new RuntimeException("customizer failure");
                        }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidYamlNoCustomizer() {
        String yaml = "name: John\nage: 30";
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertTrue(result.isSuccess(), "YAML payload should be valid");
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledWithCustomizer() {
        String json = "{\"city\":\"Paris\"}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        ValidationResult result = schema.walk(json, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) ctx -> {
                    ctx.setAttribute("skip", true);
                    ctxRef.set(ctx);
                });
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Result must be valid when validation is disabled");
        assertTrue(result.getValidationMessages().isEmpty());
        assertNotNull(ctxRef.get());
        assertEquals(true, ctxRef.get().getAttribute("skip"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkEmptyStringThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("", InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkWhitespaceOnlyThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("   \t\n", InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidUrlEncodedNoCustomizer() {
        // name=John&age=30 URL‑encoded
        String encoded = "name%3DJohn%26age%3D30";
        ValidationResult result = schema.walk(encoded, InputFormat.URL_ENCODED, true,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Decoded JSON should be valid");
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkJsonArrayFailsValidation() {
        String jsonArray = "[1, 2, 3]";
        ValidationResult result = schema.walk(jsonArray, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Array does not satisfy object schema");
        assertFalse(result.getValidationMessages().isEmpty(),
                "At least one validation message expected for type mismatch");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkConsumerSetsAttributeValidationEnabled() {
        String json = "{\"id\":10}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> {
                    ctx.setAttribute("requestId", "abc-123");
                    ctxRef.set(ctx);
                });
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Valid input should produce a valid result");
        assertTrue(result.getValidationMessages().isEmpty());
        assertNotNull(ctxRef.get());
        assertEquals("abc-123", ctxRef.get().getAttribute("requestId"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNoOpConsumerValidationEnabled() {
        String json = "{\"status\":\"ok\"}";
        AtomicBoolean invoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> invoked.set(true));
        assertTrue(invoked.get(), "Consumer must be invoked even if it does nothing");
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getValidationMessages().isEmpty());
    }
}
```