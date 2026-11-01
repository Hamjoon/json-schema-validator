```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests cover a wide range of scenarios described in the supplied JSON test matrix.
 * Real {@link JsonSchema} instances are created from JSON schema strings whenever possible.
 * For edge‑cases that require special behaviour of {@link ExecutionContext} a Mockito mock is used.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    private JsonSchema schema;               // object under test
    private ExecutionContext execContext;    // execution context used for walkAtNode
    private JsonNode node;                   // node to be validated
    private JsonNode rootNode;               // root of the document
    private JsonNodePath instanceLocation;  // location of the node inside the document

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // default schema – an empty schema that accepts anything.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema("{}");
        execContext = schema.createExecutionContext();

        // simple default node (will be overwritten in each test as needed)
        node = JsonNodeFactory.instance.textNode("default");
        rootNode = node;
        instanceLocation = JsonNodePath.root();
    }

    /* -------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------- */

    /** Create a JsonSchema from a raw JSON schema string. */
    private static JsonSchema schemaFrom(String jsonSchema) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(jsonSchema);
    }

    /** Parse a JSON document into a {@link JsonNode}. */
    private static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------
     *  1. Successful validation – simple schema
     * ------------------------------------------------- */
    @Test
    void walkAtNode_successfulSimpleSchema() {
        // schema: object with required string property "name"
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"name\":\"John\"}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "Result must be valid for a conforming instance");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* -------------------------------------------------
     *  2. Validation failure – type mismatch / missing required
     * ------------------------------------------------- */
    @Test
    void walkAtNode_missingRequiredProperty() {
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"age\":30}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Result must be invalid when required property is missing");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("required")), "Message should mention missing required property");
    }

    /* -------------------------------------------------
     *  3. Validation disabled – should skip validation
     * ------------------------------------------------- */
    @Test
    void walkAtNode_validationDisabled() {
        schema = schemaFrom("{\"type\":\"object\",\"required\":[\"name\"]}");
        execContext = schema.createExecutionContext();

        node = json("{\"any\":123}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, false);
        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is disabled");
    }

    /* -------------------------------------------------
     *  4. Null node handling
     * ------------------------------------------------- */
    @Test
    void walkAtNode_nullNodeProducesError() {
        schema = schemaFrom("{\"type\":\"object\"}");
        execContext = schema.createExecutionContext();

        node = null;
        rootNode = null;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Result must be invalid when node is null");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("null")), "Message should mention null node");
    }

    /* -------------------------------------------------
     *  5. Recursive anchor handling – no infinite recursion
     * ------------------------------------------------- */
    @Test
    void walkAtNode_recursiveAnchorHandledGracefully() {
        // Draft‑2020‑12 supports "$recursiveAnchor": true.
        String schemaJson = """
                {
                  "$recursiveAnchor": true,
                  "type": "object",
                  "properties": {
                    "self": { "$ref": "#" }
                  }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"self\":{}}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "Recursive anchor should not cause infinite recursion");
    }

    /* -------------------------------------------------
     *  6. $ref to external schema – success
     * ------------------------------------------------- */
    @Test
    void walkAtNode_externalRefSuccess() {
        // Main schema referencing external address schema via $ref
        String mainSchema = """
                {
                  "type": "object",
                  "properties": {
                    "address": { "$ref": "http://example.com/address" }
                  },
                  "required": ["address"]
                }""";

        // Mock ExecutionContext to return the external schema when the URI is requested.
        ExecutionContext mockedContext = mock(ExecutionContext.class);
        JsonSchema addressSchema = schemaFrom("""
                {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string" }
                  },
                  "required": ["street"]
                }""");
        // The internal implementation asks the ValidationContext for a schema by URI.
        // We therefore stub the call chain: executionContext.getValidationContext().getSchemaNode(...)
        ValidationContext vc = mock(ValidationContext.class);
        when(mockedContext.getValidationContext()).thenReturn(vc);
        when(vc.getSchemaNode(eq("http://example.com/address"))).thenReturn(addressSchema.getSchemaNode());

        schema = schemaFrom(mainSchema);
        execContext = mockedContext; // use the mocked context

        node = json("{\"address\":{\"street\":\"Main St\"}}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "External $ref should resolve and validate successfully");
    }

    /* -------------------------------------------------
     *  7. $ref to external schema – failure (missing required)
     * ------------------------------------------------- */
    @Test
    void walkAtNode_externalRefFailureMissingProperty() {
        String mainSchema = """
                {
                  "type": "object",
                  "properties": {
                    "address": { "$ref": "http://example.com/address" }
                  },
                  "required": ["address"]
                }""";

        ExecutionContext mockedContext = mock(ExecutionContext.class);
        JsonSchema addressSchema = schemaFrom("""
                {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string" }
                  },
                  "required": ["street"]
                }""");
        ValidationContext vc = mock(ValidationContext.class);
        when(mockedContext.getValidationContext()).thenReturn(vc);
        when(vc.getSchemaNode(eq("http://example.com/address"))).thenReturn(addressSchema.getSchemaNode());

        schema = schemaFrom(mainSchema);
        execContext = mockedContext;

        node = json("{\"address\":{\"city\":\"Metropolis\"}}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Missing required property in referenced schema must cause failure");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("street")), "Message should mention missing 'street'");
    }

    /* -------------------------------------------------
     *  8. Enum constraint – success / failure
     * ------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({
            "'[\"red\",\"green\",\"blue\"]',\"green\",true",
            "'[\"red\",\"green\",\"blue\"]',\"yellow\",false"
    })
    void walkAtNode_enumConstraint(String enumJson, String value, boolean expectedValid) {
        String schemaJson = """
                {
                  "enum": %s
                }""".formatted(enumJson);
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json('"%s"'.formatted(value));
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertEquals(expectedValid, result.isValid(),
                "Enum validation expected to be " + (expectedValid ? "valid" : "invalid"));
    }

    /* -------------------------------------------------
     *  9. oneOf – matching / non‑matching
     * ------------------------------------------------- */
    @Test
    void walkAtNode_oneOfMatching() {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "string" },
                    { "type": "integer" }
                  ]
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("42"); // integer node
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "Integer should satisfy oneOf");
    }

    @Test
    void walkAtNode_oneOfNoMatch() {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "string" },
                    { "type": "integer" }
                  ]
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("true"); // boolean node
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Boolean does not match any oneOf subschema");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("oneof")),
                "Message should indicate oneOf failure");
    }

    /* -------------------------------------------------
     * 10. Array items validation – success / failure
     * ------------------------------------------------- */
    @Test
    void walkAtNode_arrayItemsSuccess() {
        String schemaJson = """
                {
                  "type": "array",
                  "items": { "type": "integer" }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("[1,2,3]");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "All array items are integers");
    }

    @Test
    void walkAtNode_arrayItemsFailure() {
        String schemaJson = """
                {
                  "type": "array",
                  "items": { "type": "integer" }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("[1,\"two\",3]");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Array contains a non‑integer element");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("integer")),
                "Message should mention integer type mismatch");
    }

    /* -------------------------------------------------
     * 11. Instance location propagation – nested property
     * ------------------------------------------------- */
    @Test
    void walkAtNode_nestedPropertyLocation() {
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "person": {
                      "type": "object",
                      "required": ["age"],
                      "properties": {
                        "age": { "type": "integer" }
                      }
                    }
                  }
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"person\":{\"age\":\"twenty\"}}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Age is not an integer");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getPath().equals("/person/age")),
                "Message path should be '/person/age'");
    }

    /* -------------------------------------------------
     * 12. Sub‑node validation (different root)
     * ------------------------------------------------- */
    @Test
    void walkAtNode_subNodeValidation() {
        String schemaJson = """
                {
                  "type": "string",
                  "minLength": 5
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        rootNode = json("{\"title\":\"short\",\"description\":\"A longer description\"}");
        node = json("\"short\""); // the value of /title
        instanceLocation = JsonNodePath.of("/title");

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isValid(), "Sub‑node satisfies minLength constraint");
    }

    /* -------------------------------------------------
     * 13. Configuration – failFast (first error only)
     * ------------------------------------------------- */
    @Test
    void walkAtNode_failFastStopsAfterFirstError() {
        // schema with two independent failures
        String schemaJson = """
                {
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }""";
        // enable failFast via config
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, cfg);
        schema = factory.getSchema(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"age\":\"not-an-int\"}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertFalse(result.isValid(), "Result must be invalid");
        Collection<ValidationMessage> msgs = result.getMessages();
        assertEquals(1, msgs.size(), "failFast should produce exactly one message");
        // The first detected error is the missing required property "name"
        assertTrue(msgs.iterator().next().getMessage().contains("name"));
    }

    /* -------------------------------------------------
     * 14. Null parameter handling – executionContext null
     * ------------------------------------------------- */
    @Test
    void walkAtNode_nullExecutionContextThrowsNPE() {
        schema = schemaFrom("{}");
        execContext = null;
        node = json("{\"name\":\"John\"}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        assertThrows(NullPointerException.class,
                () -> schema.walkAtNode(execContext, node, rootNode, instanceLocation, true),
                "NPE expected when executionContext is null");
    }

    /* -------------------------------------------------
     * 15. Null parameter handling – instanceLocation null
     * ------------------------------------------------- */
    @Test
    void walkAtNode_nullInstanceLocationThrowsNPE() {
        schema = schemaFrom("{}");
        execContext = schema.createExecutionContext();
        node = json("{\"age\":30}");
        rootNode = node;
        instanceLocation = null;

        assertThrows(NullPointerException.class,
                () -> schema.walkAtNode(execContext, node, rootNode, instanceLocation, true),
                "NPE expected when instanceLocation is null");
    }

    /* -------------------------------------------------
     * 16. Invalid JsonNodePath – malformed pointer
     * ------------------------------------------------- */
    @Test
    void walkAtNode_malformedJsonNodePathThrows() {
        schema = schemaFrom("{}");
        execContext = schema.createExecutionContext();
        node = json("{\"name\":\"John\"}");
        rootNode = node;
        // malformed pointer: illegal "~" escape
        instanceLocation = JsonNodePath.of("/invalid~path");

        assertThrows(IllegalArgumentException.class,
                () -> schema.walkAtNode(execContext, node, rootNode, instanceLocation, true),
                "Malformed pointer should cause IllegalArgumentException");
    }

    /* -------------------------------------------------
     * 17. Unresolvable $ref fragment – IllegalArgumentException
     * ------------------------------------------------- */
    @Test
    void walkAtNode_unresolvableRefThrows() {
        String schemaJson = """
                {
                  "$ref": "#/nonexistent"
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("{\"$ref\":\"#/nonexistent\"}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        assertThrows(IllegalArgumentException.class,
                () -> schema.walkAtNode(execContext, node, rootNode, instanceLocation, true),
                "Unresolvable $ref fragment should throw IllegalArgumentException");
    }

    /* -------------------------------------------------
     * 18. ExecutionContext method throws RuntimeException
     * ------------------------------------------------- */
    @Test
    void walkAtNode_executionContextRuntimeExceptionPropagates() {
        // Use a simple schema that would normally succeed
        schema = schemaFrom("{\"type\":\"object\"}");
        // Mock ExecutionContext to throw when getValidators() is called (indirectly via walk)
        ExecutionContext badContext = mock(ExecutionContext.class);
        when(badContext.getValidators()).thenThrow(new IllegalStateException("custom failure"));

        node = json("{\"any\":123}");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.walkAtNode(badContext, node, rootNode, instanceLocation, true));
        assertEquals("custom failure", ex.getMessage());
    }

    /* -------------------------------------------------
     * 19. Unsupported format – date-time parsing error
     * ------------------------------------------------- */
    @Test
    void walkAtNode_dateTimeFormatInvalidThrows() {
        String schemaJson = """
                {
                  "type": "string",
                  "format": "date-time"
                }""";
        schema = schemaFrom(schemaJson);
        execContext = schema.createExecutionContext();

        node = json("\"not-a-date\"");
        rootNode = node;
        instanceLocation = JsonNodePath.root();

        // The library wraps DateTimeParseException in a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walkAtNode(execContext, node, rootNode, instanceLocation, true));
        assertTrue(ex.getCause() instanceof DateTimeParseException,
                "Cause should be DateTimeParseException");
    }

    // -------------------------------------------------------------------------
    // Existing simple tests from the original framework are retained unchanged.
    // -------------------------------------------------------------------------

    @Test
    void walkAtNode_returnsEmptyResultForValidNode() {
        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, true);
        assertNotNull(result, "walkAtNode must never return null");
        if (result instanceof Iterable) {
            assertFalse(((Iterable<?>) result).iterator().hasNext(),
                    "Expected no validation messages for a valid node");
        } else {
            assertTrue(result.getMessages().isEmpty(),
                    "Expected no validation messages for a valid node");
        }
    }

    @Test
    void walkAtNode_returnsResultWhenValidationDisabled() {
        ValidationResult result = schema.walkAtNode(execContext, node, rootNode, instanceLocation, false);
        assertNotNull(result, "Result must be non‑null even when validation is disabled");
        if (result instanceof Iterable) {
            assertFalse(((Iterable<?>) result).iterator().hasNext(),
                    "No messages should be produced when validation is disabled");
        } else {
            assertTrue(result.getMessages().isEmpty(),
                    "No messages should be produced when validation is disabled");
        }
    }
}
```