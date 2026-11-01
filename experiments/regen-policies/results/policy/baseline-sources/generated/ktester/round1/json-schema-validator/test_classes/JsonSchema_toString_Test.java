package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

//     @Mock
    ValidationContext // required by JsonSchema.from(...)
    validationContext;

//     @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

//     @Mock
    JsonNode // the node whose toString() we control
    schemaNode;

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

//     @BeforeEach
    void setupBeforeEach() {
        // Configure the minimal behaviour of ValidationContext used by the JsonSchema ctor.
//         when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Returning null for config avoids the creation of KeywordWalkListenerRunner.
//         when(validationContext.getConfig()).thenReturn(null);
    }

    /**
     * -----------------------------------------------------------------------
     *  Method source providing all successful test cases.
     *  ---------------------------------------------------------------------
     */
//     private static Stream<Arguments> provideSuccessfulToStringCases() {
//         return // ---------- Standard Cases ----------
//         // ---------- Standard Cases ----------
//         // ---------- Edge Cases ----------
//         // ---------- Edge Cases ----------
//         Stream.// empty path
//         of(// internal double‑quote
//         Arguments.of("/properties/name", "{\"type\":\"string\"}", "\"/properties/name\" : {\"type\":\"string\"}"), // null path
//         Arguments.of("/items", "[{\"type\":\"string\"},{\"type\":\"number\"}]", "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]"), // ---------- Special Characters ----------
//         // ---------- Whitespace and Control Characters ----------
//         Arguments.of("", "{\"type\":\"integer\"}", "\"\" : {\"type\":\"integer\"}"), // ---------- Whitespace and Control Characters ----------
//         Arguments.of("prop\"Name", "{\"type\":\"boolean\"}", "\"prop\"Name\" : {\"type\":\"boolean\"}"), // ---------- Whitespace and Control Characters ----------
//         Arguments.of(null, "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"), // ---------- Empty Nodes ----------
//         Arguments.of("/unicode/π", "{\"type\":\"number\"}", "\"/unicode/π\" : {\"type\":\"number\"}"), // ---------- Empty Nodes ----------
//         Arguments.of("/backslash\\test", "{\"type\":\"null\"}", "\"/backslash\\test\" : {\"type\":\"null\"}"), // ---------- Empty Nodes ----------
//         Arguments.of("prop'Name", "{\"type\":\"boolean\"}", "\"prop'Name\" : {\"type\":\"boolean\"}"), // ---------- Primitive Nodes ----------
//         Arguments.of("   /whitespace   ", "{\"type\":\"string\"}", "\"   /whitespace   \" : {\"type\":\"string\"}"), // ---------- Primitive Nodes ----------
//         Arguments.of("/new\nline", "{\"type\":\"boolean\"}", "\"/new\nline\" : {\"type\":\"boolean\"}"), // ---------- Long Path ----------
//         Arguments.of("/tab\tchar", "{\"type\":\"integer\"}", "\"/tab\tchar\" : {\"type\":\"integer\"}"), // ---------- Long Path ----------
//         Arguments.of("/emptyObject", "{}", "\"/emptyObject\" : {}"), // ---------- Long Path ----------
//         Arguments.of("/emptyArray", "[]", "\"/emptyArray\" : []"), // ---------- Long Path ----------
//         Arguments.of("/textNode", "\"simple text\"", "\"/textNode\" : \"simple text\""), // ---------- Literal \"null\" Path ----------
//         Arguments.of("/numericNode", "123", "\"/numericNode\" : 123"), Arguments.of("/booleanNode", "true", "\"/booleanNode\" : true"), Arguments.of("/nullNodeString", "null", "\"/nullNodeString\" : null"), Arguments.of(generateLongPath(), "{\"type\":\"string\"}", "\"" + generateLongPath() + "\" : {\"type\":\"string\"}"), Arguments.of("null", "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"));
//     }

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
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri, schemaNode, null, false);
        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(), "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
    }

    @Test
    void toString_handlesEmptySchemaNodeString() {
        // GIVEN
        String schemaPath = "#";
        // empty node string
        String nodeString = "";
        JsonNode schemaNode = nodeFromString(nodeString);
        ValidationContext validationContext = new SimpleValidationContext(null);
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
        ValidationContext validationContext = new SimpleValidationContext(null);
        URI dummyUri = URI.create("http://example.com/schema.json");
        // WHEN – create a JsonSchema whose node is null.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri, node, null, false);
        // THEN – invoking toString() must throw NPE.
        assertThrows(NullPointerException.class, jsonSchema::toString, "Calling toString() with a null schema node should throw NPE");
    }
}