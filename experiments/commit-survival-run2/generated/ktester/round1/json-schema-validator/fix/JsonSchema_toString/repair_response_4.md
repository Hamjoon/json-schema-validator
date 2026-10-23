```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#toString()}.
 *
 * The test creates a minimal {@link ValidationContext} implementation that
 * supplies only the methods required by {@link JsonSchema#from(...)}.
 */
class JsonSchema_toString_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal {@link ValidationContext} implementation required for the
     * {@link JsonSchema#from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)}
     * factory method.
     */
    private static class SimpleValidationContext extends ValidationContext {

        SimpleValidationContext(JsonMetaSchema metaSchema) {
            // The current constructor of ValidationContext expects factories,
            // a meta‑schema, a JsonSchemaFactory and a config.  For the purpose
            // of these tests we can safely pass {@code null} for everything
            // except the meta‑schema.
            super(null, null, metaSchema, null, null);
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            // No real meta‑schema is needed for the tests.
            return null;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            // Returning {@code null} disables the creation of a
            // KeywordWalkListenerRunner, matching the original test intent.
            return null;
        }
    }

    /**
     * Helper to create a {@link JsonNode} from a JSON string.  For the
     * empty‑string case a tiny custom {@link JsonNode} implementation is
     * returned that simply yields the empty string from {@code toString()}.
     */
    private static JsonNode nodeFromString(String json) {
        if (json == null) {
            return null;
        }
        if (json.isEmpty()) {
            // Represents an empty node – the toString() of MissingNode is "".
            return MissingNode.getInstance();
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void toString_returnsExpectedRepresentation() {
        // GIVEN
        String schemaPath = "/properties/name";
        String nodeString = "{\"type\":\"string\"}";
        JsonNode schemaNode = nodeFromString(nodeString);
        ValidationContext validationContext = new SimpleValidationContext(null);
        URI dummyUri = URI.create("http://example.com/schema.json");

        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri,
                schemaNode, null, false);

        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
    }

    @Test
    void toString_handlesEmptySchemaNodeString() {
        // GIVEN
        String schemaPath = "#";
        String nodeString = "";                 // empty node string
        JsonNode schemaNode = nodeFromString(nodeString);
        ValidationContext validationContext = new SimpleValidationContext(null);
        URI dummyUri = URI.create("http://example.com/empty.json");

        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri,
                schemaNode, null, false);

        // THEN
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "toString() should correctly handle an empty node string");
    }

    @Test
    void toString_nullSchemaNode_throwsNullPointerException() {
        // GIVEN
        String schemaPath = "/nullNode";
        JsonNode node = null;
        ValidationContext validationContext = new SimpleValidationContext(null);
        URI dummyUri = URI.create("http://example.com/schema.json");

        // WHEN – create a JsonSchema whose node is null.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri,
                node, null, false);

        // THEN – invoking toString() must throw NPE.
        assertThrows(NullPointerException.class, jsonSchema::toString,
                "Calling toString() with a null schema node should throw NPE");
    }
}
```