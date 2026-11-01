```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)}.
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

        ValidationResult result = schema.walk(executionContext, invalidJson,
                InputFormat.JSON, true, customizer);

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        // The schema defines a type mismatch, therefore at least one message is expected.
        assertFalse(messages.isEmpty(), "There should be at least one validation message");
        // For completeness, ensure the message mentions the property "name".
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getPath().contains("name"));
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

        ValidationResult result = schema.walk(executionContext, invalidJson,
                InputFormat.JSON, false, customizer);

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Messages set must not be null");
        // Validation is disabled, so the message set must be empty.
        assertTrue(messages.isEmpty(), "Messages should be empty when validation is disabled");
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