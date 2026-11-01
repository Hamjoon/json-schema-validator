```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * The tests create a simple {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * then invoke the {@code validate} method with a custom {@link OutputFormat}
 * implementation that converts the {@link Set} of {@link ValidationMessage}s
 * into a {@link List}. Both a failing validation and a successful validation
 * scenario are covered.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /** Shared factory used to create schemas for all tests. */
    private static JsonSchemaFactory factory;

    /** The schema under test – an object with a required {@code name} property of type {@code string}. */
    private static JsonSchema schema;

    /** Simple JSON schema used in the tests. */
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

    /** A minimal {@link OutputFormat} that returns the received messages as a {@link List}. */
    private static final class ListOutputFormat implements OutputFormat<List<ValidationMessage>> {
        @Override
        public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
            return new ArrayList<>(messages);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory once for the whole test class.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Build a JsonSchema instance from the simple schema string.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally – the factory does not hold external resources.
    }

    /**
     * Validates an input that violates the schema (numeric {@code name}) and checks that the
     * custom {@link OutputFormat} receives exactly one validation message.
     */
    @Test
    void validateWithOutputFormatReturnsValidationMessages() {
        // Input where the required property {@code name} has the wrong type.
        String invalidInput = """
                {
                  "name": 123
                }
                """;

        OutputFormat<List<ValidationMessage>> format = new ListOutputFormat();

        List<ValidationMessage> result = schema.validate(invalidInput, InputFormat.JSON, format);

        // The schema should produce a single type‑mismatch message.
        assertEquals(1, result.size(), "Exactly one validation message is expected");
        assertTrue(result.get(0).getMessage().toLowerCase().contains("type"),
                "The message should indicate a type problem");
    }

    /**
     * Validates a correct input and verifies that the {@link OutputFormat} receives an empty set.
     */
    @Test
    void validateWithOutputFormatWhenInputIsValidReturnsEmptyList() {
        // Valid input that satisfies the schema.
        String validInput = """
                {
                  "name": "John Doe"
                }
                """;

        OutputFormat<List<ValidationMessage>> format = new ListOutputFormat();

        List<ValidationMessage> result = schema.validate(validInput, InputFormat.JSON, format);

        // No validation errors should be produced.
        assertTrue(result.isEmpty(), "No validation messages should be returned for valid input");
    }
}
```