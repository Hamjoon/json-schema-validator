```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, boolean)}.
 *
 * The tests create a {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * feed it with a simple schema and a couple of JSON instances, and verify the
 * {@link ValidationResult} returned by {@code walk}.
 */
class JsonSchema_walk_2_Test {

    private static ObjectMapper mapper;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // ObjectMapper is thread‑safe after configuration, so a single instance is fine.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that expects an object with a string property "name".
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

        // Use the factory to obtain a JsonSchema instance. No special loaders are needed.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test class.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Walk a valid JSON node with validation enabled.
     * Expect an empty set of validation messages.
     */
    @Test
    void walkValidNodeShouldReturnNoMessages() throws Exception {
        // {"name":"John"}
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");

        ValidationResult result = schema.walk(validNode, true);

        // ValidationResult#getMessages() returns a Set<ValidationMessage>
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid instance");
    }

    /**
     * Walk an invalid JSON node with validation enabled.
     * The property "name" is a number, which violates the schema.
     * Expect at least one validation message.
     */
    @Test
    void walkInvalidNodeShouldReturnMessages() throws Exception {
        // {"name":123}
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");

        ValidationResult result = schema.walk(invalidNode, true);

        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");
        // Optionally verify that the message mentions the type mismatch.
        boolean containsTypeError = messages.stream()
                .anyMatch(msg -> msg.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑related validation message");
    }

    /**
     * Walk a valid JSON node with validation disabled.
     * The method should still return a {@link ValidationResult} but without performing
     * any validation, i.e., the messages set must be empty.
     */
    @Test
    void walkValidNodeWithoutValidationShouldReturnNoMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");

        ValidationResult result = schema.walk(validNode, false);

        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(),
                "When validation is disabled, walk should return an empty message set");
    }
}
```