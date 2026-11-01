package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
     */
    @BeforeEach
    void setupBeforeEach() {
        // Very small schema: an object with a required string property "name".
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
        // No special configuration is required for these tests.
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created successfully");
    }

    /**
     * Verify that {@code walk(..., validate = true)} returns a {@link ValidationResult}
     * containing the expected validation error when the input does not satisfy the schema.
     */
    @Test
    void walk_WithValidationErrors_ShouldReturnMessages() {
        // Input where the required property "name" is present but of the wrong type (number).
        String invalidJson = """
            { "name": 123 }
            """;
        ExecutionContext executionContext = new ExecutionContext();
        // No customisation required for this test – use a Mockito mock to satisfy the signature.
        ExecutionContextCustomizer customizer = Mockito.mock(ExecutionContextCustomizer.class);
        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, true, customizer);
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        // The schema defines a type mismatch, therefore at least one message is expected.
        assertFalse(messages.isEmpty(), "There should be at least one validation message");
        // For completeness, ensure the message mentions the property "name".
        boolean mentionsName = messages.stream().anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "Validation message should refer to the 'name' property");
    }

    /**
     * Verify that {@code walk(..., validate = false)} does not perform validation
     * and therefore returns an empty set of messages.
     */
    @Test
    void walk_WithValidateFlagFalse_ShouldReturnEmptyMessages() {
        // Same malformed JSON as the previous test – we expect no validation because the flag is false.
        String invalidJson = """
            { "name": 123 }
            """;
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = Mockito.mock(ExecutionContextCustomizer.class);
        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, false, customizer);
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        // Validation is disabled, so the message set must be empty.
        assertTrue(messages.isEmpty(), "Messages should be empty when validation is disabled");
    }

    @Test
    void walk_ValidJson_DefaultContextAndCustomizer_ShouldSucceed() {
        // Happy‑path: well‑formed JSON, validation enabled, no customisation.
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
            // no‑op customiser
        };
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "A valid JSON document must produce an empty validation message set");
    }

    @Test
    void walk_ValidYaml_CustomExecutionContext_ShouldSucceed() {
        // Use a custom ExecutionContext with a different draft version.
        String yaml = "name: John\nage: 30";
        ExecutionContext executionContext = ExecutionContext.builder().setVersion(VersionFlag.V201909).build();
        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(new SchemaValidatorsConfig());
        ValidationResult result = schema.walk(executionContext, yaml, InputFormat.YAML, true, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Valid YAML input should validate successfully");
    }

    @Test
    void walk_InvalidJson_DeserializationFailure_ShouldThrowException() {
        // Malformed JSON (missing quotes around value) – expect a parsing exception.
        String malformed = "{\"name\":John}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        Exception ex = assertThrows(Exception.class, () -> schema.walk(executionContext, malformed, InputFormat.JSON, true, customizer));
        // The library wraps parsing errors in a ValidationException (runtime), but we accept any exception.
        assertNotNull(ex.getMessage());
    }

    @Test
    void walk_UnsupportedEncoding_ShouldThrowIllegalArgumentException() {
        // URL‑encoded input with an illegal percent‑encoding sequence.
        // incomplete UTF‑8 sequence
        String badEncoded = "%E0%A4%A";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(IllegalArgumentException.class, () -> schema.walk(executionContext, badEncoded, InputFormat.URL_ENCODED, true, customizer));
    }

    @Test
    void walk_ValidateFlagFalse_ShouldSkipValidation() {
        // Input violates the schema but validation flag is false.
        String json = "{\"age\":\"twenty\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, false, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled, the result must contain no messages");
    }

    @Test
    void walk_NullExecutionContext_ShouldCreateDefaultContext() {
        // Pass null as the execution context – the method should handle it gracefully.
        String json = "{\"name\":1}";
        ExecutionContextCustomizer customizer = ctx -> {
        };
        ValidationResult result = schema.walk(null, json, InputFormat.JSON, true, customizer);
        assertNotNull(result);
        // The JSON is valid (name is a number? actually schema expects string, so error)
        // The important part is that we get a result without NPE.
        assertNotNull(result.getMessages());
    }

    @Test
    void walk_NullCustomizer_ShouldProceedWithNoOpCustomizer() {
        String json = "{\"type\":\"object\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, null);
        assertNotNull(result);
        // The JSON does not contain required "name", so we expect an error.
        assertFalse(result.getMessages().isEmpty(), "Even with a null customizer, validation should be performed");
    }

    @Test
    void walk_EmptyJsonString_ShouldThrowException() {
        String empty = "";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(Exception.class, () -> schema.walk(executionContext, empty, InputFormat.JSON, true, customizer));
    }

    @Test
    void walk_NullInputString_ShouldThrowNullPointerException() {
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, InputFormat.JSON, true, customizer));
    }

    @Test
    void walk_CustomizerThrowsRuntimeException_ShouldPropagate() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, json, InputFormat.JSON, true, failingCustomizer));
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
        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true));
        ValidationResult result = complex.walk(executionContext, json, InputFormat.JSON, true, customizer);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "With failFast enabled, only the first validation error should be reported");
    }

    @Test
    void walk_ValidUrlEncodedInput_ShouldValidateSuccessfully() {
        // Percent‑encoded JSON representing {"name":"John"}
        String encoded = "%7B%22name%22%3A%22John%22%7D";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        ValidationResult result = schema.walk(executionContext, encoded, InputFormat.URL_ENCODED, true, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "A correctly URL‑encoded JSON document should validate without errors");
    }

    @Test
    void walk_ValidateFlagFalse_WithCustomExecutionContext_ShouldSkipValidation() {
        String json = "{\"age\":\"twenty\"}";
        ExecutionContext executionContext = ExecutionContext.builder().setVersion(VersionFlag.V202012).build();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, false, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Even with a custom ExecutionContext, validation must be skipped when flag is false");
    }

    @Test
    void walk_NullInputFormat_ShouldThrowIllegalArgumentException() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(IllegalArgumentException.class, () -> schema.walk(executionContext, json, null, true, customizer));
    }

    @Test
    void walk_InvalidYaml_DeserializationFailure_ShouldThrowException() {
        // Invalid YAML (non‑numeric value for an integer field – but our schema doesn't define integers;
        // we just need a syntactically invalid YAML).
        // 'age' is not defined, but syntax is fine.
        String badYaml = "name: John\nage: thirty";
        // To force a parsing error, we use an obviously malformed YAML:
        String malformedYaml = "name: John\n: bad";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(Exception.class, () -> schema.walk(executionContext, malformedYaml, InputFormat.YAML, true, customizer));
    }

    @Test
    void walk_MalformedUrlEncodedInput_ShouldThrowIllegalArgumentException() {
        // Incomplete percent‑encoding at the end.
        String malformed = "%7B%22name%22%3A%22John";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(IllegalArgumentException.class, () -> schema.walk(executionContext, malformed, InputFormat.URL_ENCODED, true, customizer));
    }

    @Test
    void walk_CustomizerThrowsAssertionError_ShouldPropagate() {
        String json = "{\"name\":\"John\"}";
        ExecutionContext executionContext = new ExecutionContext();
        ExecutionContextCustomizer customizer = ctx -> {
            throw new AssertionError("customizer assertion failure");
        };
        AssertionError err = assertThrows(AssertionError.class, () -> schema.walk(executionContext, json, InputFormat.JSON, true, customizer));
        assertEquals("customizer assertion failure", err.getMessage());
    }
}
