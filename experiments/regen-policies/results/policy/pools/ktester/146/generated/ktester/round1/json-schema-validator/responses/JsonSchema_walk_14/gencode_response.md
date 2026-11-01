```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The tests cover a wide range of scenarios described in the JSON test case specification,
 * including successful validation, validation skipping, deserialization failures,
 * handling of null arguments, custom execution contexts, customizers that throw
 * exceptions, fail‑fast configuration and URL‑encoded inputs.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_14_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;

    /**
     * Initialise the {@link JsonSchemaFactory} once for the whole test class.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent stable version in the library.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Build a fresh {@link JsonSchema} instance before each test.
     * The default schema validates an object that must contain a required string property "name".
     */
    @BeforeEach
    void setupBeforeEach() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created successfully");
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests (kept unchanged)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_WithValidationErrors_ShouldReturnMessages() {
        String invalidJson = """
                { "name": 123 }
                """;

        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = Mockito.mock(ExecutionContextCustomizer.class);

        ValidationResult result = schema.walk(executionContext, invalidJson,
                InputFormat.JSON, true, customizer);

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        assertFalse(messages.isEmpty(), "There should be at least one validation message");
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "Validation message should refer to the 'name' property");
    }

    @Test
    void walk_WithValidateFlagFalse_ShouldReturnEmptyMessages() {
        String invalidJson = """
                { "name": 123 }
                """;

        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = Mockito.mock(ExecutionContextCustomizer.class);

        ValidationResult result = schema.walk(executionContext, invalidJson,
                InputFormat.JSON, false, customizer);

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        assertTrue(messages.isEmpty(), "Messages should be empty when validation is disabled");
    }

    /* --------------------------------------------------------------------- */
    /* New tests derived from the provided JSON test case specification       */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_ValidJson_DefaultContextAndCustomizer_ShouldSucceed() {
        // Happy‑path: well‑formed JSON, validation enabled, no customisation.
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
            // no‑op customiser
        };

        ValidationResult result = schema.walk(executionContext, json,
                InputFormat.JSON, true, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "A valid JSON document must produce an empty validation message set");
    }

    @Test
    void walk_ValidYaml_CustomExecutionContext_ShouldSucceed() {
        // Use a custom ExecutionContext with a different draft version.
        String yaml = "name: John\nage: 30";
        ExecutionContext executionContext = ExecutionContext.builder()
                .setVersion(VersionFlag.V201909)
                .build();

        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(new SchemaValidatorsConfig());

        ValidationResult result = schema.walk(executionContext, yaml,
                InputFormat.YAML, true, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "Valid YAML input should validate successfully");
    }

    @Test
    void walk_InvalidJson_DeserializationFailure_ShouldThrowException() {
        // Malformed JSON (missing quotes around value) – expect a parsing exception.
        String malformed = "{\"name\":John}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        Exception ex = assertThrows(Exception.class, () ->
                schema.walk(executionContext, malformed, InputFormat.JSON, true, customizer));

        // The library wraps parsing errors in a ValidationException (runtime), but we accept any exception.
        assertNotNull(ex.getMessage());
    }

    @Test
    void walk_UnsupportedEncoding_ShouldThrowIllegalArgumentException() {
        // URL‑encoded input with an illegal percent‑encoding sequence.
        String badEncoded = "%E0%A4%A"; // incomplete UTF‑8 sequence
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(IllegalArgumentException.class, () ->
                schema.walk(executionContext, badEncoded, InputFormat.URL_ENCODED, true, customizer));
    }

    @Test
    void walk_ValidateFlagFalse_ShouldSkipValidation() {
        // Input violates the schema but validation flag is false.
        String json = "{\"age\":\"twenty\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        ValidationResult result = schema.walk(executionContext, json,
                InputFormat.JSON, false, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "When validation is disabled, the result must contain no messages");
    }

    @Test
    void walk_NullExecutionContext_ShouldCreateDefaultContext() {
        // Pass null as the execution context – the method should handle it gracefully.
        String json = "{\"name\":1}";
        ExecutionContextCustomizer customizer = ctx -> {};

        ValidationResult result = schema.walk(null, json,
                InputFormat.JSON, true, customizer);

        assertNotNull(result);
        // The JSON is valid (name is a number? actually schema expects string, so error)
        // The important part is that we get a result without NPE.
        assertNotNull(result.getMessages());
    }

    @Test
    void walk_NullCustomizer_ShouldProceedWithNoOpCustomizer() {
        String json = "{\"type\":\"object\"}";
        ExecutionContext executionContext = new ExecutionContext();

        ValidationResult result = schema.walk(executionContext, json,
                InputFormat.JSON, true, null);

        assertNotNull(result);
        // The JSON does not contain required "name", so we expect an error.
        assertFalse(result.getMessages().isEmpty(),
                "Even with a null customizer, validation should be performed");
    }

    @Test
    void walk_EmptyJsonString_ShouldThrowException() {
        String empty = "";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(Exception.class, () ->
                schema.walk(executionContext, empty, InputFormat.JSON, true, customizer));
    }

    @Test
    void walk_NullInputString_ShouldThrowNullPointerException() {
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(NullPointerException.class, () ->
                schema.walk(executionContext, null, InputFormat.JSON, true, customizer));
    }

    @Test
    void walk_CustomizerThrowsRuntimeException_ShouldPropagate() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();

        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                schema.walk(executionContext, json, InputFormat.JSON, true, failingCustomizer));

        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void walk_FailFastConfiguration_ShouldReturnSingleError() {
        // Build a schema that can produce more than one error.
        String complexSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age": { "type": "integer" }
                  },
                  "required": ["name", "age"]
                }
                """;
        JsonSchema complex = factory.getSchema(complexSchema);

        // Input violates both constraints: name is a number, age is a string.
        String json = "{\"name\":123,\"age\":\"twenty\"}";

        ExecutionContext executionContext = ExecutionContext.builder().build();
        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(
                new SchemaValidatorsConfig().setFailFast(true));

        ValidationResult result = complex.walk(executionContext, json,
                InputFormat.JSON, true, customizer);

        assertNotNull(result);
        Set<ValidationMessage> messages = result.getMessages();
        assertEquals(1, messages.size(),
                "With failFast enabled, only the first validation error should be reported");
    }

    @Test
    void walk_ValidUrlEncodedInput_ShouldValidateSuccessfully() {
        // Percent‑encoded JSON representing {"name":"John"}
        String encoded = "%7B%22name%22%3A%22John%22%7D";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        ValidationResult result = schema.walk(executionContext, encoded,
                InputFormat.URL_ENCODED, true, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "A correctly URL‑encoded JSON document should validate without errors");
    }

    @Test
    void walk_ValidateFlagFalse_WithCustomExecutionContext_ShouldSkipValidation() {
        String json = "{\"age\":\"twenty\"}";
        ExecutionContext executionContext = ExecutionContext.builder()
                .setVersion(VersionFlag.V202012)
                .build();
        ExecutionContextCustomizer customizer = ctx -> {};

        ValidationResult result = schema.walk(executionContext, json,
                InputFormat.JSON, false, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "Even with a custom ExecutionContext, validation must be skipped when flag is false");
    }

    @Test
    void walk_NullInputFormat_ShouldThrowIllegalArgumentException() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(IllegalArgumentException.class, () ->
                schema.walk(executionContext, json, null, true, customizer));
    }

    @Test
    void walk_InvalidYaml_DeserializationFailure_ShouldThrowException() {
        // Invalid YAML (non‑numeric value for an integer field – but our schema doesn't define integers;
        // we just need a syntactically invalid YAML).
        String badYaml = "name: John\nage: thirty"; // 'age' is not defined, but syntax is fine.
        // To force a parsing error, we use an obviously malformed YAML:
        String malformedYaml = "name: John\n: bad";

        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(Exception.class, () ->
                schema.walk(executionContext, malformedYaml, InputFormat.YAML, true, customizer));
    }

    @Test
    void walk_MalformedUrlEncodedInput_ShouldThrowIllegalArgumentException() {
        // Incomplete percent‑encoding at the end.
        String malformed = "%7B%22name%22%3A%22John";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {};

        assertThrows(IllegalArgumentException.class, () ->
                schema.walk(executionContext, malformed, InputFormat.URL_ENCODED, true, customizer));
    }

    @Test
    void walk_CustomizerThrowsAssertionError_ShouldPropagate() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();

        ExecutionContextCustomizer customizer = ctx -> {
            throw new AssertionError("customizer assertion failure");
        };

        AssertionError err = assertThrows(AssertionError.class, () ->
                schema.walk(executionContext, json, InputFormat.JSON, true, customizer));

        assertEquals("customizer assertion failure", err.getMessage());
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
    }
}
```