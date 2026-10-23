package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    @Mock
    ValidationContext // required by JsonSchema.from(...)
    validationContext;

    @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    @Mock
    JsonNode // the node whose toString() we control
    schemaNode;

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

    @BeforeEach
    void setupBeforeEach() {
        // Configure the minimal behaviour of ValidationContext used by the JsonSchema ctor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Returning null for config avoids the creation of KeywordWalkListenerRunner.
        when(validationContext.getConfig()).thenReturn(null);
    }

    /**
     * -----------------------------------------------------------------------
     *  Method source providing all successful test cases.
     *  ---------------------------------------------------------------------
     */
    private static Stream<Arguments> provideSuccessfulToStringCases() {
        return // ---------- Standard Cases ----------
        Stream.// ---------- Standard Cases ----------
        of(// ---------- Edge Cases ----------
        Arguments.of("/properties/name", "{\"type\":\"string\"}", "\"/properties/name\" : {\"type\":\"string\"}"), // ---------- Edge Cases ----------
        Arguments.of("/items", "[{\"type\":\"string\"},{\"type\":\"number\"}]", "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]"), // empty path
        // internal double‑quote
        Arguments.of("", "{\"type\":\"integer\"}", "\"\" : {\"type\":\"integer\"}"), // null path
        Arguments.of("prop\"Name", "{\"type\":\"boolean\"}", "\"prop\"Name\" : {\"type\":\"boolean\"}"), // ---------- Special Characters ----------
        Arguments.of(null, "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"), // ---------- Whitespace and Control Characters ----------
        Arguments.of("/unicode/π", "{\"type\":\"number\"}", "\"/unicode/π\" : {\"type\":\"number\"}"), // ---------- Whitespace and Control Characters ----------
        Arguments.of("/backslash\\test", "{\"type\":\"null\"}", "\"/backslash\\test\" : {\"type\":\"null\"}"), // ---------- Whitespace and Control Characters ----------
        Arguments.of("prop'Name", "{\"type\":\"boolean\"}", "\"prop'Name\" : {\"type\":\"boolean\"}"), // ---------- Empty Nodes ----------
        Arguments.of("   /whitespace   ", "{\"type\":\"string\"}", "\"   /whitespace   \" : {\"type\":\"string\"}"), // ---------- Empty Nodes ----------
        Arguments.of("/new\nline", "{\"type\":\"boolean\"}", "\"/new\nline\" : {\"type\":\"boolean\"}"), // ---------- Empty Nodes ----------
        Arguments.of("/tab\tchar", "{\"type\":\"integer\"}", "\"/tab\tchar\" : {\"type\":\"integer\"}"), // ---------- Primitive Nodes ----------
        Arguments.of("/emptyObject", "{}", "\"/emptyObject\" : {}"), // ---------- Primitive Nodes ----------
        Arguments.of("/emptyArray", "[]", "\"/emptyArray\" : []"), // ---------- Long Path ----------
        Arguments.of("/textNode", "\"simple text\"", "\"/textNode\" : \"simple text\""), // ---------- Long Path ----------
        Arguments.of("/numericNode", "123", "\"/numericNode\" : 123"), // ---------- Long Path ----------
        Arguments.of("/booleanNode", "true", "\"/booleanNode\" : true"), // ---------- Long Path ----------
        Arguments.of("/nullNodeString", "null", "\"/nullNodeString\" : null"), // ---------- Literal \"null\" Path ----------
        Arguments.of(generateLongPath(), "{\"type\":\"string\"}", "\"" + generateLongPath() + "\" : {\"type\":\"string\"}"), Arguments.of("null", "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"));
    }

    /**
     * Helper to generate a path longer than 100 characters.
     */
    private static String generateLongPath() {
        // 110 characters (including the leading slash)
        return "/" + "a".repeat(109);
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
            // toString() returns ""
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
        ValidationContext validationContext = new SimpleValidationContext(JsonMetaSchema.getInstance());
        URI dummyUri = URI.create("http://example.com/schema.json");
        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri, // no parent schema
        schemaNode, null, false);
        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(), "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
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
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri, schemaNode, null, false);
        // THEN
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(), "toString() should correctly handle an empty node string");
    }

    /**
     * -----------------------------------------------------------------------
     *  Exception case – null schema node should cause NPE.
     *  ---------------------------------------------------------------------
     */
    @Test
    void toString_nullSchemaNode_throwsNullPointerException() {
        // GIVEN
        String schemaPath = "/nullNode";
        JsonNode node = null;
        ValidationContext validationContext = new SimpleValidationContext(JsonMetaSchema.getInstance());
        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, URI.create("http://example.com/schema.json"), node, null, false);
        // THEN
        assertThrows(NullPointerException.class, jsonSchema::toString, "Calling toString() with a null schema node should throw NPE");
    }
}
