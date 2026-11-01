```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat)}.
 */
class JsonSchema_validate_6_Test {

    private static JsonSchemaFactory factory;
    private static JsonSchema schema;

    /**
     * Initialise shared resources once for all tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent stable version in the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No special configuration needed for these simple tests.
        });

        // A tiny schema that requires an object with a required string property "name".
        String schemaData = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;

        // Default configuration – do not preload any external schemas.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        schema = factory.getSchema(schemaData, config);
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
        // Release static references – helps the GC when many test classes are run.
        factory = null;
        schema = null;
    }

    /**
     * A valid JSON document that satisfies the schema should produce an empty
     * validation result.
     */
    @Test
    void validate_validInput_returnsEmptySet() {
        String validJson = """
                {
                  "name": "John Doe"
                }
                """;

        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid document");
    }

    /**
     * An invalid JSON document (wrong type for {@code name}) should yield a
     * single validation message.
     */
    @Test
    void validate_invalidInput_returnsMessages() {
        String invalidJson = """
                {
                  "name": 123
                }
                """;

        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON);
        assertEquals(1, messages.size(), "Expected exactly one validation message for type mismatch");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"),
                "Message should indicate a type error");
    }
}
```