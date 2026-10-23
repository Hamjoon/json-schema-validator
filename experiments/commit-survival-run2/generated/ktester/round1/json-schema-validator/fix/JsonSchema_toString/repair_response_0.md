```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#toString()}.
 * <p>
 * The original version of this test used Mockito for creating mocks of
 * {@link ValidationContext}, {@link JsonMetaSchema} and {@link JsonNode}.
 * The project does not have a Mockito dependency, therefore the test has been
 * rewritten to avoid any Mockito usage.  Simple stub implementations and
 * real {@link JsonNode} instances created via {@link ObjectMapper} are used
 * instead.
 * </p>
 */
class JsonSchema_toString_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Provides a variety of successful {@code toString()} cases.
     */
    private static Stream<Arguments> provideSuccessfulToStringCases() {
        return Stream.of(
                // ---------- Standard Cases ----------
                Arguments.of("/properties/name", "{\"type\":\"string\"}",
                        "\"/properties/name\" : {\"type\":\"string\"}"),
                Arguments.of("/items", "[{\"type\":\"string\"},{\"type\":\"number\"}]",
                        "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]"),
                // ---------- Edge Cases ----------
                // empty path
                Arguments.of("", "{\"type\":\"integer\"}",
                        "\"\" : {\"type\":\"integer\"}"),
                // internal double‑quote
                Arguments.of("prop\"Name", "{\"type\":\"boolean\"}",
                        "\"prop\"Name\" : {\"type\":\"boolean\"}"),
                // null path
                Arguments.of(null, "{\"type\":\"string\"}",
                        "\"null\" : {\"type\":\"string\"}"),
                // ---------- Special Characters ----------
                Arguments.of("/unicode/π", "{\"type\":\"number\"}",
                        "\"/unicode/π\" : {\"type\":\"number\"}"),
                Arguments.of("/backslash\\test", "{\"type\":\"null\"}",
                        "\"/backslash\\test\" : {\"type\":\"null\"}"),
                Arguments.of("prop'Name", "{\"type\":\"boolean\"}",
                        "\"prop'Name\" : {\"type\":\"boolean\"}"),
                // ---------- Whitespace and Control Characters ----------
                Arguments.of("   /whitespace   ", "{\"type\":\"string\"}",
                        "\"   /whitespace   \" : {\"type\":\"string\"}"),
                Arguments.of("/new\nline", "{\"type\":\"boolean\"}",
                        "\"/new\nline\" : {\"type\":\"boolean\"}"),
                Arguments.of("/tab\tchar", "{\"type\":\"integer\"}",
                        "\"/tab\tchar\" : {\"type\":\"integer\"}"),
                // ---------- Empty Nodes ----------
                Arguments.of("/emptyObject", "{}", "\"/emptyObject\" : {}"),
                Arguments.of("/emptyArray", "[]", "\"/emptyArray\" : []"),
                // ---------- Primitive Nodes ----------
                Arguments.of("/textNode", "\"simple text\"",
                        "\"/textNode\" : \"simple text\""),
                Arguments.of("/numericNode", "123", "\"/numericNode\" : 123"),
                Arguments.of("/booleanNode", "true", "\"/booleanNode\" : true"),
                Arguments.of("/nullNodeString", "null", "\"/nullNodeString\" : null"),
                // ---------- Long Path ----------
                Arguments.of(generateLongPath(), "{\"type\":\"string\"}",
                        "\"" + generateLongPath() + "\" : {\"type\":\"string\"}"),
                // ---------- Literal \"null\" Path ----------
                Arguments.of("null", "{\"type\":\"string\"}",
                        "\"null\" : {\"type\":\"string\"}")
        );
    }

    /**
     * Helper to generate a path longer than 100 characters.
     */
    private static String generateLongPath() {
        // 110 characters (including the leading slash)
        return "/" + "a".repeat(109);
    }

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
        @Override public void addKeywordValidator(String keyword, KeywordValidator validator) { throw new UnsupportedOperationException(); }
        @Override public JsonSchemaFactory getJsonSchemaFactory() { throw new UnsupportedOperationException(); }
        @Override public boolean isReadOnly() { throw new UnsupportedOperationException(); }
        @Override public void setReadOnly(boolean readOnly) { throw new UnsupportedOperationException(); }
        @Override public JsonSchemaFactory getFactory() { throw new UnsupportedOperationException(); }
        @Override public void setFactory(JsonSchemaFactory factory) { throw new UnsupportedOperationException(); }
        @Override public JsonNode getRootNode() { throw new UnsupportedOperationException(); }
        @Override public void setRootNode(JsonNode rootNode) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getRootSchema() { throw new UnsupportedOperationException(); }
        @Override public void setRootSchema(JsonSchema rootSchema) { throw new UnsupportedOperationException(); }
        @Override public void setMetaSchema(JsonMetaSchema metaSchema) { throw new UnsupportedOperationException(); }
        @Override public void setConfig(SchemaValidatorsConfig config) { throw new UnsupportedOperationException(); }
        @Override public void putSchema(String uri, JsonSchema schema) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getSchema(String uri) { throw new UnsupportedOperationException(); }
    }

    /**
     * Creates a {@link JsonNode} from a JSON string.  For the empty‑string case
     * a tiny custom {@link JsonNode} implementation is returned that simply
     * yields the empty string from {@code toString()}.
     */
    private static JsonNode nodeFromString(String json) {
        if (json == null) {
            return null;
        }
        if (json.isEmpty()) {
            // Very small JsonNode that only overrides toString().
            return new JsonNode() {
                @Override public String toString() { return ""; }
                // All other abstract methods throw UnsupportedOperationException.
                @Override public JsonNode get(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode path(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode get(int index) { throw new UnsupportedOperationException(); }
                @Override public JsonNode path(int index) { throw new UnsupportedOperationException(); }
                @Override public JsonNode at(String jsonPtrExpr) { throw new UnsupportedOperationException(); }
                @Override public JsonNode at(JsonPointer ptr) { throw new UnsupportedOperationException(); }
                @Override public JsonParser.NumberType numberType() { throw new UnsupportedOperationException(); }
                @Override public Number numberValue() { throw new UnsupportedOperationException(); }
                @Override public short shortValue() { throw new UnsupportedOperationException(); }
                @Override public int intValue() { throw new UnsupportedOperationException(); }
                @Override public long longValue() { throw new UnsupportedOperationException(); }
                @Override public BigDecimal decimalValue() { throw new UnsupportedOperationException(); }
                @Override public double doubleValue() { throw new UnsupportedOperationException(); }
                @Override public float floatValue() { throw new UnsupportedOperationException(); }
                @Override public boolean booleanValue() { throw new UnsupportedOperationException(); }
                @Override public byte[] binaryValue() { throw new UnsupportedOperationException(); }
                @Override public JsonNode deepCopy() { throw new UnsupportedOperationException(); }
                @Override public JsonNode findValue(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) { throw new UnsupportedOperationException(); }
                @Override public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) { throw new UnsupportedOperationException(); }
                @Override public JsonNode findParent(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) { throw new UnsupportedOperationException(); }
                @Override public JsonNode findPath(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode get(String fieldName, JsonNode defaultNode) { throw new UnsupportedOperationException(); }
                @Override public JsonNode get(int index, JsonNode defaultNode) { throw new UnsupportedOperationException(); }
                @Override public JsonNode required(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode required(int index) { throw new UnsupportedOperationException(); }
                @Override public JsonNode path(String fieldName, JsonNode defaultNode) { throw new UnsupportedOperationException(); }
                @Override public JsonNode path(int index, JsonNode defaultNode) { throw new UnsupportedOperationException(); }
                @Override public boolean isMissingNode() { return false; }
                @Override public boolean isArray() { return false; }
                @Override public boolean isObject() { return false; }
                @Override public boolean isValueNode() { return false; }
                @Override public boolean isContainerNode() { return false; }
                @Override public boolean isPojo() { return false; }
                @Override public boolean isNumber() { return false; }
                @Override public boolean isIntegralNumber() { return false; }
                @Override public boolean isFloatingPointNumber() { return false; }
                @Override public boolean isTextual() { return false; }
                @Override public boolean isBinary() { return false; }
                @Override public boolean isBoolean() { return false; }
                @Override public boolean isNull() { return false; }
                @Override public boolean asBoolean() { throw new UnsupportedOperationException(); }
                @Override public boolean asBoolean(boolean defaultValue) { throw new UnsupportedOperationException(); }
                @Override public int asInt() { throw new UnsupportedOperationException(); }
                @Override public int asInt(int defaultValue) { throw new UnsupportedOperationException(); }
                @Override public long asLong() { throw new UnsupportedOperationException(); }
                @Override public long asLong(long defaultValue) { throw new UnsupportedOperationException(); }
                @Override public double asDouble() { throw new UnsupportedOperationException(); }
                @Override public double asDouble(double defaultValue) { throw new UnsupportedOperationException(); }
                @Override public String asText() { return ""; }
                @Override public String asText(String defaultValue) { return ""; }
                @Override public JsonToken asToken() { throw new UnsupportedOperationException(); }
                @Override public JsonParser.NumberType numberType(JsonParser.NumberType defaultNumberType) { throw new UnsupportedOperationException(); }
                @Override public JsonPointer at(JsonPointer ptr) { throw new UnsupportedOperationException(); }
                @Override public JsonPointer at(String jsonPtrExpr) { throw new UnsupportedOperationException(); }
                @Override public JsonNode with(String propertyName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode withArray(String propertyName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode without(String propertyName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode findParent(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) { throw new UnsupportedOperationException(); }
                @Override public JsonNode findPath(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public JsonNode findValue(String fieldName) { throw new UnsupportedOperationException(); }
                @Override public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) { throw new UnsupportedOperationException(); }
                @Override public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) { throw new UnsupportedOperationException(); }
            };
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
        ValidationContext validationContext = new SimpleValidationContext(new JsonMetaSchema());

        URI dummyUri = URI.create("http://example.com/schema.json");

        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext,
                schemaPath,
                dummyUri,
                schemaNode,
                null,          // no parent schema
                false);

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
        ValidationContext validationContext = new SimpleValidationContext(new JsonMetaSchema());

        URI dummyUri = URI.create("http://example.com/empty.json");

        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext,
                schemaPath,
                dummyUri,
                schemaNode,
                null,
                false);

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
        ValidationContext validationContext = new SimpleValidationContext(new JsonMetaSchema());

        // WHEN / THEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext,
                schemaPath,
                URI.create("http://example.com/schema.json"),
                node,
                null,
                false);

        assertThrows(NullPointerException.class, jsonSchema::toString,
                "Calling toString() with a null schema node should throw NPE");
    }
}
```