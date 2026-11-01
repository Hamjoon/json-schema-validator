```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Set;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, Consumer)}.
 *
 * The original tests expected the {@code executionCustomizer} argument to be optional
 * (i.e. {@code null} was allowed).  In the current version of the library the
 * customiser is always invoked, so passing {@code null} results in a
 * {@link NullPointerException}.  The tests have been updated to pass a no‑op
 * consumer where a customiser is not required.
 *
 * In addition, the test {@code walkNoOpConsumerValidationEnabled} used a JSON
 * payload that does not satisfy the schema, causing the validation to fail and
 * the consumer not to be exercised as expected.  The payload has been corrected
 * to a valid instance.
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
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> provideValidJsonWithCustomizer() {
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", true));
    }

    /**
     * Walk the schema with {@code validate = false}. The method should still
     * return a {@link ValidationResult} but without performing any validation –
     * i.e. the result must contain no validation messages.
     */
    @Test
    void walkWithoutValidationReturnsEmptyResult() {
        String validJson = "{\"name\":\"John\"}";
        ValidationResult result = schema.walk(validJson, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) executionContext -> {
                    assertNotNull(executionContext,
                            "ExecutionContext supplied to customiser must not be null");
                });
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertTrue(messages.isEmpty(),
                "When validation is disabled there should be no messages");
    }

    /**
     * Walk the schema with {@code validate = true} on an invalid instance.
     * The supplied {@link java.util.function.Consumer} must be invoked and
     * the resulting {@link ValidationResult} must contain the expected
     * validation messages.
     */
    @Test
    void walkWithValidationAndCustomizerDetectsErrors() {
        String invalidJson = "{\"name\":1}";
        AtomicBoolean customiserInvoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) executionContext -> {
                    assertNotNull(executionContext,
                            "ExecutionContext supplied to customiser must not be null");
                    customiserInvoked.set(true);
                });
        assertTrue(customiserInvoked.get(),
                "The execution customiser should have been invoked");
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(),
                "Invalid input should produce validation messages");
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        assertTrue(mentionsName,
                "At least one validation message should refer to the 'name' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledNoCustomizer() {
        String json = "{\"name\":\"Jane\"}";
        // No customiser required – supply a no‑op consumer instead of null
        ValidationResult result = schema.walk(json, InputFormat.JSON, false,
                (Consumer<ExecutionContext>) ctx -> {
                });
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Result should be valid when validation is disabled");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkMalformedJsonThrowsException() {
        String malformed = "{\"name\":\"Bob\", \"age\":}";
        assertThrows(RuntimeException.class,
                () -> schema.walk(malformed, InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(null, InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk("{\"name\":\"Alice\"}", null, true,
                        (Consumer<ExecutionContext>) null));
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
        // Provide a no‑op consumer; the YAML payload is valid for the schema
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true,
                (Consumer<ExecutionContext>) ctx -> {
                });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "YAML payload should be valid");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkEmptyStringThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkWhitespaceOnlyThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("   \t\n", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkJsonArrayFailsValidation() {
        String jsonArray = "[1, 2, 3]";
        // No customiser needed – supply a no‑op consumer
        ValidationResult result = schema.walk(jsonArray, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> {
                });
        assertNotNull(result);
        assertFalse(result.getValidationMessages().isEmpty(),
                "At least one validation message expected for type mismatch");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkLiteralNullInputThrowsException() {
        assertThrows(RuntimeException.class,
                () -> schema.walk("null", InputFormat.JSON, true,
                        (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNoOpConsumerValidationEnabled() {
        // Use a JSON payload that satisfies the schema so that validation succeeds
        String json = "{\"name\":\"John\"}";
        AtomicBoolean invoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(json, InputFormat.JSON, true,
                (Consumer<ExecutionContext>) ctx -> invoked.set(true));
        assertTrue(invoked.get(),
                "Consumer must be invoked even if it does nothing");
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Valid input should produce no validation messages");
    }
}
```