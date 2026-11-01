```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /** Simple schema used by the tests. */
    private static final String SIMPLE_SCHEMA = "{"
            + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "   \"name\":{\"type\":\"string\"}"
            + "},"
            + "\"required\":[\"name\"]"
            + "}";

    /** JSON input that conforms to {@link #SIMPLE_SCHEMA}. */
    private static final String VALID_INPUT = "{\"name\":\"John\"}";

    /** JSON input that violates {@link #SIMPLE_SCHEMA}. */
    private static final String INVALID_INPUT = "{\"name\":1}";

    private static JsonSchemaFactory factory;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory that can create JsonSchema instances.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh JsonSchema instance for every test to avoid state‑leakage.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop the reference – not strictly required but keeps the
        // test class tidy and mirrors typical cleanup patterns.
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    @Test
    void walk_validInput_returnsNoValidationMessages() {
        // given
        String input = VALID_INPUT;
        InputFormat format = InputFormat.JSON;
        boolean validate = true;

        // when
        ValidationResult result = schema.walk(input, format, validate);

        // then
        assertNotNull(result, "Result should never be null");
        // The ValidationResult API provides a collection of messages; an empty
        // collection means the input is valid.
        assertTrue(result.getMessages().isEmpty(),
                () -> "Expected no validation messages, but got: " + result.getMessages());
    }

    @Test
    void walk_invalidInput_returnsValidationMessages() {
        // given
        String input = INVALID_INPUT;
        InputFormat format = InputFormat.JSON;
        boolean validate = true;

        // when
        ValidationResult result = schema.walk(input, format, validate);

        // then
        assertNotNull(result, "Result should never be null");
        // The schema requires \"name\" to be a string, so we expect at least one message.
        assertFalse(result.getMessages().isEmpty(),
                "Expected validation messages for an invalid input");
    }
}
```