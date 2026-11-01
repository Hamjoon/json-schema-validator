```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean)}.
 *
 * The tests create a real {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * provide a simple schema and JSON input, and verify that the {@code walk} method
 * returns a {@link ValidationResult} containing the expected validation messages.
 *
 * No private members of {@link JsonSchema} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_7_Test {

    /** Shared factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** The schema under test. */
    private JsonSchema schema;

    /** Execution context supplied to the {@code walk} method. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft (the most recent at the time of writing)
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Simple schema:
         * {
         *   "$schema": "https://json-schema.org/draft/2020-12/schema",
         *   "type": "object",
         *   "properties": {
         *     "name": { "type": "string" }
         *   },
         *   "required": [ "name" ]
         * }
         */
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        // No special configuration – the default is fine for these tests
        schema = factory.getSchema(schemaJson);

        // A fresh execution context for each test ensures isolation.
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for the current test scenario.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Factory does not hold state that needs explicit disposal.
    }

    /**
     * Verify that {@code walk} validates the input JSON and returns a
     * {@link ValidationResult} containing the expected validation message
     * when the input does not satisfy the schema.
     */
    @Test
    void walkShouldReturnValidationResultWhenInputIsInvalid() {
        // Input where the required property "name" is of the wrong type (number instead of string)
        String invalidJson = """
                {
                  "name": 123
                }
                """;

        // Invoke the method under test
        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, true);

        // Basic sanity checks
        assertNotNull(result, "walk should never return null");

        // The result should contain at least one validation message describing the type error.
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "ValidationResult#getMessages() must not return null");
        assertFalse(messages.isEmpty(), "Expected validation messages for invalid input");

        // Optional: check that the message mentions the property "name"
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /**
     * Verify that {@code walk} returns an empty {@link ValidationResult}
     * when the input JSON satisfies the schema.
     */
    @Test
    void walkShouldReturnEmptyResultWhenInputIsValid() {
        // Valid input matching the schema
        String validJson = """
                {
                  "name": "John Doe"
                }
                """;

        ValidationResult result = schema.walk(executionContext, validJson, InputFormat.JSON, true);

        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "ValidationResult#getMessages() must not return null");
        assertTrue(messages.isEmpty(), "No validation messages are expected for valid input");
    }

    /**
     * Verify that the {@code validate} flag set to {@code false} short‑circuits
     * validation – the returned {@link ValidationResult} should contain no messages
     * even if the input is invalid.
     */
    @Test
    void walkShouldSkipValidationWhenValidateFlagIsFalse() {
        String invalidJson = """
                {
                  "name": 123
                }
                """;

        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, false);

        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "ValidationResult#getMessages() must not return null");
        assertTrue(messages.isEmpty(),
                "When the validate flag is false, walk should return an empty ValidationResult");
    }
}
```