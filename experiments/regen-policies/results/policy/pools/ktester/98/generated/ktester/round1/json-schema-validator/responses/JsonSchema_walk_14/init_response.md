```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The tests focus on the public contract of the method:
 *   – it should deserialize the supplied JSON string,
 *   – it should walk the schema tree and return a {@link ValidationResult},
 *   – the {@link ValidationResult} must contain validation messages when the input does not satisfy the schema.
 *
 * All required collaborators are created using the public API (no reflection or private‑member access).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_14_Test {

    /** A simple schema used for all test cases. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;

    private JsonSchema schema;               // the object under test
    private ExecutionContext executionContext; // fresh context for each test

    /** Create a {@link JsonSchema} instance once for the whole test class. */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed at the moment.
    }

    /** Build a fresh {@link JsonSchema} and {@link ExecutionContext} before each test. */
    @BeforeEach
    void setupBeforeEach() {
        // Obtain a JsonSchemaFactory for the 2020‑12 meta‑schema.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // The factory creates a thread‑safe JsonSchema instance.
        schema = factory.getSchema(SIMPLE_SCHEMA);
        // A plain ExecutionContext is sufficient for these tests.
        executionContext = new ExecutionContext();
    }

    /** Clean up references after each test – helps the GC when many tests run. */
    @AfterEach
    void teardownAfterEach() {
        schema = null;
        executionContext = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Walk a *valid* JSON document. The result must contain no validation messages.
     */
    @Test
    void walkValidInput_returnsEmptyResult() {
        // GIVEN
        String validJson = """{ "name": "John Doe" }""";
        ExecutionContextCustomizer customizer = ctx -> {
            // No customisation needed for this test – the lambda simply satisfies the API.
        };

        // WHEN
        ValidationResult result = schema.walk(
                executionContext,
                validJson,
                InputFormat.JSON,
                true,                     // ask the walk to perform validation
                customizer);

        // THEN
        assertNotNull(result, "walk should never return null");
        // ValidationResult exposes the messages via getMessages(); an empty set means success.
        assertTrue(result.getMessages().isEmpty(),
                () -> "Expected no validation messages, but got: " + result.getMessages());
    }

    /**
     * Walk an *invalid* JSON document. The result must contain at least one validation message.
     */
    @Test
    void walkInvalidInput_returnsMessages() {
        // GIVEN
        // The schema requires a string for the property "name", but we supply a number.
        String invalidJson = """{ "name": 123 }""";
        ExecutionContextCustomizer customizer = ctx -> {
            // No customisation needed; the lambda is only required to match the functional interface.
        };

        // WHEN
        ValidationResult result = schema.walk(
                executionContext,
                invalidJson,
                InputFormat.JSON,
                true,
                customizer);

        // THEN
        assertNotNull(result, "walk should never return null");
        assertFalse(result.getMessages().isEmpty(),
                "Expected validation messages for an invalid document");
        // Optionally verify that the message mentions the property "name".
        boolean mentionsName = result.getMessages().stream()
                .anyMatch(msg -> msg.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /**
     * Walk with validation disabled (the {@code validate} flag set to {@code false}).
     * The method must still return a {@link ValidationResult} but it should contain no messages
     * even if the input is invalid, because validation was skipped.
     */
    @Test
    void walkWithValidationDisabled_returnsEmptyResultEvenForInvalidInput() {
        // GIVEN
        String invalidJson = """{ "name": 123 }""";
        ExecutionContextCustomizer customizer = ctx -> {};

        // WHEN
        ValidationResult result = schema.walk(
                executionContext,
                invalidJson,
                InputFormat.JSON,
                false,        // validation disabled
                customizer);

        // THEN
        assertNotNull(result, "walk should never return null");
        assertTrue(result.getMessages().isEmpty(),
                "When validation is disabled, the result must contain no messages");
    }
}
```