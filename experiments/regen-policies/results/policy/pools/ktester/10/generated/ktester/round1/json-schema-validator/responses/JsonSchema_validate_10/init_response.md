```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_10_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonSchema jsonSchema;
    private ExecutionContextCustomizer noopCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a simple schema that requires a "name" property of type string
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // Use the factory to obtain a JsonSchema instance (the factory hides the
        // construction details of ValidationContext, SchemaLocation, etc.)
        jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                                      .getSchema(schemaNode);

        // A no‑op customizer – the lambda matches the functional interface
        // ExecutionContextCustomizer (its single method customizes the ExecutionContext)
        noopCustomizer = executionContext -> {
            // intentionally left blank
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    @Test
    void validate_ValidJson_ReturnsEmptyMessages() {
        String validJson = """{ "name": "John Doe" }""";

        Set<ValidationMessage> messages = jsonSchema.validate(validJson,
                                                              InputFormat.JSON,
                                                              noopCustomizer);

        assertTrue(messages.isEmpty(),
                   () -> "Expected no validation messages for valid JSON, but got: " + messages);
    }

    @Test
    void validate_InvalidJson_ReturnsRequiredPropertyMessage() {
        String invalidJson = """{ "age": 30 }"""; // missing required "name"

        Set<ValidationMessage> messages = jsonSchema.validate(invalidJson,
                                                              InputFormat.JSON,
                                                              noopCustomizer);

        assertFalse(messages.isEmpty(), "Expected validation messages for invalid JSON");
        // At least one message should mention the missing required property "name"
        boolean containsRequired = messages.stream()
                .anyMatch(m -> m.getMessage().contains("required") && m.getMessage().contains("name"));
        assertTrue(containsRequired,
                () -> "Expected a required‑property message for 'name', but messages were: " + messages);
    }
}
```