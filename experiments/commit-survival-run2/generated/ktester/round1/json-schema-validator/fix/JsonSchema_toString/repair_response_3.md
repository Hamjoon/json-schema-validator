```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#toString()}.
 * <p>
 * The original test used Mockito to create a {@link ValidationContext} mock.
 * To keep the test self‑contained (the project does not depend on Mockito),
 * a very small concrete {@code ValidationContext} implementation is provided.
 */
class JsonSchema_toString_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal {@link ValidationContext} implementation required for
     * {@link JsonSchema#from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)}.
     * <p>
     * The real {@code ValidationContext} in the library is an abstract class,
     * therefore we extend it and implement only the abstract methods that the
     * {@code JsonSchema} constructor calls (currently {@link #getMetaSchema()}
     * and {@link #getConfig()}).
     */
    private static class SimpleValidationContext extends ValidationContext {

        SimpleValidationContext(JsonMetaSchema metaSchema) {
            super(metaSchema);
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            // The test does not need a real meta‑schema, so we simply return null.
            return null;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            // Returning {@code null} disables the creation of a KeywordWalkListenerRunner,
            // which matches the original test intent.
            return null;
        }

        // All other methods are not used by the tests; they throw
        // UnsupportedOperationException if invoked.
        @Override
        public void addKeywordValidator(String keyword, KeywordValidator validator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchemaFactory getJsonSchemaFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchemaFactory getFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFactory(JsonSchemaFactory factory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getRootNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRootNode(JsonNode rootNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getRootSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRootSchema(JsonSchema rootSchema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMetaSchema(JsonMetaSchema metaSchema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setConfig(SchemaValidatorsConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putSchema(String uri, JsonSchema schema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getSchema(String uri) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Helper to create a {@link JsonNode} from a JSON string.
     * For an empty string a {@link MissingNode} is returned.
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
        String nodeString = ""; // empty node string
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