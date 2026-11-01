```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean)}.
 *
 * The tests create a simple JSON schema, deserialize a JSON instance that
 * violates the schema and verify that {@code walk(...)} returns a
 * {@link ValidationResult} containing the expected validation messages.
 *
 * No private members of {@link JsonSchema} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_3_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Initialise the {@link JsonSchemaFactory} once for all tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 meta‑schema (draft‑2020‑12) – the same version used in the
        // existing test suite.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /**
     * Build a {@link JsonSchema} instance before each test.
     *
     * The schema validates an object that must contain a {@code name} property of
     * type {@code string}.
     */
    @BeforeEach
    void setupBeforeEach() {
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

        // No special configuration – default validation behaviour.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        schema = factory.getSchema(schemaJson, config);
        assertNotNull(schema, "Schema should be created successfully");
    }

    /**
     * Clean up after each test – nothing specific is required because the
     * {@link JsonSchema} instance is immutable after construction.
     */
    @AfterEach
    void teardownAfterEach() {
        // No resources to release.
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, boolean)} correctly
     * validates a JSON instance and returns a {@link ValidationResult}
     * containing a single validation error (the {@code name} property is not a
     * string).
     */
    @Test
    void walk_returnsValidationResultWithErrors() throws Exception {
        // JSON instance where "name" is an integer – violates the schema.
        String instanceJson = """
                {
                  "name": 123
                }
                """;

        JsonNode instanceNode = MAPPER.readTree(instanceJson);
        ExecutionContext ctx = schema.createExecutionContext();

        // Invoke the focal method.
        ValidationResult result = schema.walk(ctx, instanceNode, true);

        // Basic sanity checks.
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "There should be at least one validation message");

        // The schema requires "name" to be a string, so we expect exactly one message.
        assertEquals(1, messages.size(), "Exactly one validation error is expected");

        ValidationMessage msg = messages.iterator().next();
        // The message text varies with locale; we only assert that it mentions the property.
        assertTrue(msg.getMessage().contains("name"),
                "Validation message should refer to the property 'name'");
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }
}
```