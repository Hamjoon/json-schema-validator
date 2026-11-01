```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    private static Stream<Arguments> provideValidJsonWithCustomizer() {
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", true));
    }

    @Test
    void walkWithoutValidationReturnsEmptyResult() {
        String validJson = "{\"name\":\"John\"}";
        ValidationResult result = schema.walk(validJson, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) executionContext -> {
                    assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
                });
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertTrue(messages.isEmpty(), "When validation is disabled there should be no messages");
    }

    @Test
    void walkWithValidationAndCustomizerDetectsErrors() {
        String invalidJson = "{\"name\":1}";
        AtomicBoolean customiserInvoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) executionContext -> {
                    assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
                    customiserInvoked.set(true);
                });
        assertTrue(customiserInvoked.get(), "The execution customiser should have been invoked");
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Invalid input should produce validation messages");
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        assertTrue(mentionsName, "At least one validation message should refer to the 'name' property");
    }

    @Test
    void walkValidJsonWithCustomizerValidationEnabled() {
        String json = "{\"name\":\"John\",\"age\":30}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> {
                    ctx.setAttribute("custom", Boolean.TRUE);
                    ctxRef.set(ctx);
                });
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(), "Result should be valid for correct JSON");
        assertNotNull(ctxRef.get(), "Consumer must have received a non‑null ExecutionContext");
        assertEquals(Boolean.TRUE, ctxRef.get().getAttribute("custom"),
                "Custom attribute should be set by the consumer");
    }

    @Test
    void walkValidJsonValidationDisabledNoCustomizer() {
        String json = "{\"name\":\"Jane\"}";
        ValidationResult result = schema.walk(json, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Result should be valid when validation is disabled");
    }

    @Test
    void walkMalformedJsonThrowsException() {
        String malformed = "{\"name\":\"Bob\", \"age\":}";
        assertThrows(RuntimeException.class,
                () -> schema.walk(malformed, InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    @Test
    void walkNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(null, InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    @Test
    void walkNullInputFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk("{\"name\":\"Alice\"}", null, true,
                        (Consumer<ExecutionContext>) null));
    }

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

    @Test
    void walkValidYamlNoCustomizer() {
        String yaml = "name: John\nage: 30";
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "YAML payload should be valid");
    }

    @Test
    void walkValidJsonValidationDisabledWithCustomizer() {
        String json = "{\"city\":\"Paris\"}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        ValidationResult result = schema.walk(json, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) ctx -> {
                    ctx.setAttribute("skip", Boolean.TRUE);
                    ctxRef.set(ctx);
                });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Result must be valid when validation is disabled");
        assertNotNull(ctxRef.get());
        assertEquals(Boolean.TRUE, ctxRef.get().getAttribute("skip"));
    }

    @Test
    void walkEmptyStringThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    @Test
    void walkWhitespaceOnlyThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("   \t\n", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    @Test
    void walkJsonArrayFailsValidation() {
        String jsonArray = "[1, 2, 3]";
        ValidationResult result = schema.walk(jsonArray, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertFalse(result.getValidationMessages().isEmpty(),
                "At least one validation message expected for type mismatch");
    }

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
        assertTrue(result.getValidationMessages().isEmpty(),
                "Valid input should produce a valid result");
        assertNotNull(ctxRef.get());
        assertEquals("abc-123", ctxRef.get().getAttribute("requestId"));
    }

    @Test
    void walkNoOpConsumerValidationEnabled() {
        String json = "{\"status\":\"ok\"}";
        AtomicBoolean invoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> invoked.set(true));
        assertTrue(invoked.get(), "Consumer must be invoked even if it does nothing");
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    void walkLiteralNullInputThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("null", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }
}
```