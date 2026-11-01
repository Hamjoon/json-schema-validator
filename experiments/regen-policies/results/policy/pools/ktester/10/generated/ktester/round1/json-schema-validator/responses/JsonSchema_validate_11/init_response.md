```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * The test creates a real {@link JsonSchema} instance using a minimal JSON
 * schema, validates a JSON document and verifies that the supplied
 * {@link OutputFormat} is invoked with the expected validation messages.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_11_Test {

    /** The schema used in the tests. */
    private JsonSchema schema;

    /** Simple JSON schema: an object with a required string property {@code name}. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance from the simple schema string.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * A tiny {@link OutputFormat} implementation that returns a string describing
     * the number of validation messages it received.
     */
    private static class MessageCountingOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            return "count:" + messages.size();
        }
    }

    @Test
    void validate_ValidJson_ReturnsZeroMessageCount() {
        // GIVEN
        String validJson = """{ "name": "John Doe" }""";
        InputFormat inputFormat = InputFormat.JSON;               // standard JSON input
        OutputFormat<String> outputFormat = new MessageCountingOutputFormat();

        // WHEN
        String result = schema.validate(validJson, inputFormat, outputFormat);

        // THEN
        assertEquals("count:0", result, "A valid JSON document should produce zero validation messages");
    }

    @Test
    void validate_InvalidJson_ReturnsNonZeroMessageCount() {
        // GIVEN
        // Missing the required {@code name} property.
        String invalidJson = """{ "age": 30 }""";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new MessageCountingOutputFormat();

        // WHEN
        String result = schema.validate(invalidJson, inputFormat, outputFormat);

        // THEN
        // The schema requires the {@code name} property, therefore at least one message.
        assertTrue(result.startsWith("count:"), "Result should start with the prefix \"count:\"");
        int count = Integer.parseInt(result.substring("count:".length()));
        assertTrue(count > 0, "Invalid JSON should yield a positive number of validation messages");
    }
}
```