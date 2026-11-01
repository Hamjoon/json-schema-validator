```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The test builds a simple schema using {@link JsonSchemaFactory} and then invokes
 * {@code walkAtNode} directly.  The schema requires a property {@code name} of type
 * {@code string}.  The supplied JSON instance contains an integer for {@code name},
 * therefore validation must fail and the returned {@link ValidationResult} must contain
 * at least one {@link ValidationMessage}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    /** Shared objects for all tests. */
    private static JsonSchemaFactory factory;
    private static JsonSchema schema;
    private static ObjectMapper mapper;

    /** Instance specific objects. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // a minimal schema that validates an object with a required string property "name"
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ],
                  "additionalProperties": false
                }
                """;

        // Build the factory for the 2020‑12 draft (the most recent draft supported by the library)
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // No special configuration is required for this test
        schema = factory.getSchema(schemaJson);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh execution context for each test – the default constructor is sufficient
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up for now
    }

    /**
     * Verify that {@code walkAtNode} returns a {@link ValidationResult}
     * containing validation messages when the supplied JSON does not satisfy the schema.
     */
    @Test
    void walkAtNode_invalidInstance_returnsValidationMessages() throws Exception {
        // --------------------------------------------------------------------
        // given – a JSON instance that violates the schema (name is an integer)
        // --------------------------------------------------------------------
        String instanceJson = """
                {
                  "name": 123
                }
                """;
        JsonNode node = mapper.readTree(instanceJson);
        JsonNode rootNode = node; // for this test the node is also the root

        // JsonNodePath requires a PathType – we use the JSON_POINTER type which is the
        // default for normal validation walks.
        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // --------------------------------------------------------------------
        // when – the method under test is invoked
        // --------------------------------------------------------------------
        ValidationResult result = schema.walkAtNode(executionContext, node, rootNode, instanceLocation, true);

        // --------------------------------------------------------------------
        // then – a non‑null result containing at least one validation message
        // --------------------------------------------------------------------
        assertNotNull(result, "walkAtNode should never return null");
        // ValidationResult stores the messages in a Set; the exact accessor name may vary
        // between library versions, but {@code getMessages()} is the public API.
        assertFalse(result.getMessages().isEmpty(),
                "Expected validation errors because the instance does not conform to the schema");
    }

    /**
     * Verify that a valid instance yields an empty set of validation messages.
     */
    @Test
    void walkAtNode_validInstance_returnsNoMessages() throws Exception {
        // --------------------------------------------------------------------
        // given – a JSON instance that satisfies the schema
        // --------------------------------------------------------------------
        String instanceJson = """
                {
                  "name": "John Doe"
                }
                """;
        JsonNode node = mapper.readTree(instanceJson);
        JsonNode rootNode = node;
        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // --------------------------------------------------------------------
        // when
        // --------------------------------------------------------------------
        ValidationResult result = schema.walkAtNode(executionContext, node, rootNode, instanceLocation, true);

        // --------------------------------------------------------------------
        // then
        // --------------------------------------------------------------------
        assertNotNull(result, "walkAtNode should never return null");
        assertTrue(result.getMessages().isEmpty(),
                "A valid instance should produce no validation messages");
    }
}
```