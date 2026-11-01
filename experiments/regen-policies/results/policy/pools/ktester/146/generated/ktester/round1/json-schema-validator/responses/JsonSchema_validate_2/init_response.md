```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode)}.
 *
 * <p>The tests create a {@link JsonSchema} instance via {@link JsonSchemaFactory}
 * (the public way of obtaining a schema) and then invoke {@code validate(JsonNode)} 
 * with both valid and invalid JSON payloads. No private members are accessed and
 * all dependencies are created through the public API.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    /** ObjectMapper used to build {@link JsonNode}s from JSON strings. */
    private static ObjectMapper mapper;

    /** The schema under test. */
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a shared ObjectMapper – thread‑safe for read‑only use.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /* -------------------------------------------------------------
         * Build a simple schema:
         *
         * {
         *   "$schema": "https://json-schema.org/draft/2020-12/schema",
         *   "type": "object",
         *   "properties": {
         *     "name": { "type": "string" }
         *   },
         *   "required": ["name"]
         * }
         *
         * The JsonSchemaFactory is the supported entry point for creating a
         * JsonSchema instance.  No reflection or private constructors are used.
         * ------------------------------------------------------------- */
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Use the default configuration – it is sufficient for these tests.
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created by the factory");
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
        mapper = null;
    }

    /**
     * Verify that {@code validate(JsonNode)} returns a non‑empty set when the
     * supplied JSON does not satisfy the schema (wrong type for {@code name}).
     */
    @Test
    void validate_invalidJsonNode_returnsValidationMessages() throws Exception {
        // JSON where "name" is a number – violates the "type":"string" rule.
        String invalidJson = "{ \"name\": 123 }";
        JsonNode node = mapper.readTree(invalidJson);

        Set<ValidationMessage> messages = schema.validate(node);

        assertNotNull(messages, "Resulting set must not be null");
        assertFalse(messages.isEmpty(), "There should be at least one validation error");
        // The exact message text may vary with locale; we only assert that a
        // message concerning the property "name" is present.
        boolean containsName = messages.stream()
                .anyMatch(m -> m.getPath().equals("name"));
        assertTrue(containsName, "Validation errors should reference the 'name' property");
    }

    /**
     * Verify that a valid JSON document yields an empty validation result.
     */
    @Test
    void validate_validJsonNode_returnsEmptySet() throws Exception {
        String validJson = "{ \"name\": \"John Doe\" }";
        JsonNode node = mapper.readTree(validJson);

        Set<ValidationMessage> messages = schema.validate(node);

        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation errors");
    }
}
```