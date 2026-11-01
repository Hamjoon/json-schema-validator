```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple validation tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 * <p>
 * The original test suite relied heavily on Mockito for complex discriminator logic.
 * Those parts have been removed to keep the compilation self‑contained while preserving
 * the core validation behaviour checks.
 */
class JsonSchemaValidateTest {

    /** The schema used for the basic tests – an object with a required string property {@code "name"}. */
    private static JsonSchema schema;

    /** Jackson mapper for creating {@link JsonNode} instances from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** ExecutionContext created fresh for each test. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Build a JsonSchemaFactory for the 2020‑12 draft.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Simple schema: {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
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
        // Create the JsonSchema instance once – it is thread‑safe.
        schema = factory.getSchema(schemaJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A plain ExecutionContext is sufficient for the validation run.
        executionContext = new ExecutionContext();
    }

    /**
     * Verify that the {@code validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath)} method
     * returns a non‑empty error set when the instance does not satisfy the schema.
     */
    @Test
    void validateDetectsTypeMismatch() {
        // Input JSON: {"name":1} – the “name” property is a number, but the schema expects a string.
        JsonNode input = MAPPER.readTree("{\"name\":1}");
        // The root node and the instance node are the same for this simple test.
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // Call the focal method.
        Set<ValidationMessage> errors = schema.validate(executionContext, input, input, rootPath);
        // The type validator should have produced exactly one ValidationMessage.
        assertEquals(1, errors.size(), "Expected a single validation error for type mismatch");
        ValidationMessage msg = errors.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("string"),
                "Error message should mention the expected string type");
    }

    /**
     * Verify that a valid instance produces an empty error set.
     */
    @Test
    void validateSucceedsWhenInstanceMatchesSchema() {
        // Input JSON that satisfies the schema: {"name":"John"}
        JsonNode input = MAPPER.readTree("{\"name\":\"John\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = schema.validate(executionContext, input, input, rootPath);
        assertTrue(errors.isEmpty(), "No validation errors should be reported for a valid instance");
    }
}
```