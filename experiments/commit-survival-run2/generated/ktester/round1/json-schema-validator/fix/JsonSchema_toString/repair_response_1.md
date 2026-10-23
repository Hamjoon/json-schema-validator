```java
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#toString()}.
 */
class JsonSchema_toString_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal {@link ValidationContext} implementation required for
     * {@link JsonSchema#from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)}.
     */
    private static class SimpleValidationContext implements ValidationContext {

        private final JsonMetaSchema metaSchema;

        SimpleValidationContext(JsonMetaSchema metaSchema) {
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            // Returning {@code null} disables the creation of a KeywordWalkListenerRunner,
            // which matches the original test intent.
            return null;
        }

        // The remaining methods of ValidationContext are not used by the tests.
        // They are left with default implementations that throw
        // {@link UnsupportedOperationException} if invoked.
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
     * Creates a {@link JsonNode} from a JSON string.  For the empty‑string case
     * a {@link MissingNode} is returned which yields an empty string from {@code toString()}.
     */
    private static JsonNode nodeFromString(String json) {
        if (json == null) {
            return null;
        }
        if (json.isEmpty()) {
            return MissingNode.getInstance(); // toString() returns ""
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
        ValidationContext validationContext = new SimpleValidationContext(JsonMetaSchema.getInstance());
        URI dummyUri = URI.create("http://example.com/schema.json");

        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri,
                schemaNode, // no parent schema
                null, false);

        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
    }

    @Test
    void toString_handlesEmptySchemaNodeString() {
        // GIVEN
        String schemaPath = "#";
        String nodeString = "";
        JsonNode schemaNode = nodeFromString(nodeString);
        ValidationContext validationContext = new SimpleValidationContext(JsonMetaSchema.getInstance());
        URI dummyUri = URI.create("http://example.com/empty.json");

        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri,
                schemaNode, null, false);

        // THEN
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "toString() should correctly handle an empty node string");
    }

    /**
     * Exception case – null schema node should cause NPE.
     */
    @Test
    void toString_nullSchemaNode_throwsNullPointerException() {
        // GIVEN
        String schemaPath = "/nullNode";
        JsonNode node = null;
        ValidationContext validationContext = new SimpleValidationContext(JsonMetaSchema.getInstance());

        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath,
                URI.create("http://example.com/schema.json"), node, null, false);

        // THEN
        assertThrows(NullPointerException.class, jsonSchema::toString,
                "Calling toString() with a null schema node should throw NPE");
    }
}
```