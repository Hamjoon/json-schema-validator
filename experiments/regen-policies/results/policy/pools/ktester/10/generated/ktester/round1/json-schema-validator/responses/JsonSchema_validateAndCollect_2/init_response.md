```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_2_Test {

    /** Object mapper used to build JSON nodes for the tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The JsonSchema instance under test. */
    private JsonSchema jsonSchema;

    /** A simple valid JSON instance used in the tests. */
    private JsonNode validInstance;

    /** An invalid JSON instance used in the tests. */
    private JsonNode invalidInstance;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------
         * Build a very small schema:
         *   {
         *     "type": "object",
         *     "required": ["id"],
         *     "properties": {
         *       "id": { "type": "integer" },
         *       "name": { "type": "string" }
         *     }
         *   }
         * -------------------------------------------------------------- */
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["id"],
                  "properties": {
                    "id": { "type": "integer" },
                    "name": { "type": "string" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // The factory creates a JsonSchema instance from the schema node.
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaNode);

        // Build a valid instance that satisfies the schema.
        String validJson = """
                {
                  "id": 10,
                  "name": "Alice"
                }
                """;
        validInstance = MAPPER.readTree(validJson);

        // Build an invalid instance (missing required "id").
        String invalidJson = """
                {
                  "name": "Bob"
                }
                """;
        invalidInstance = MAPPER.readTree(invalidJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – helps the GC when many tests run.
        jsonSchema = null;
        validInstance = null;
        invalidInstance = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void validateAndCollect_WithValidNode_ReturnsSuccessfulResult() {
        // GIVEN – a JsonSchema instance (created in @BeforeEach) and a valid JSON node.

        // WHEN
        ValidationResult result = jsonSchema.validateAndCollect(validInstance);

        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isValid(), "Result should be valid for a conforming instance");
        assertEquals(0, result.getMessages().size(),
                "There should be no validation messages for a valid instance");
    }

    @Test
    void validateAndCollect_WithInvalidNode_ReturnsFailedResult() {
        // GIVEN – a JsonSchema instance and an invalid JSON node (missing required property).

        // WHEN
        ValidationResult result = jsonSchema.validateAndCollect(invalidInstance);

        // THEN
        assertNotNull(result, "Result must not be null");
        assertFalse(result.isValid(), "Result should be invalid when required properties are missing");
        assertTrue(result.getMessages().stream()
                        .anyMatch(msg -> msg.getMessage().contains("required")),
                "Validation messages should contain information about the missing required property");
    }
}
```