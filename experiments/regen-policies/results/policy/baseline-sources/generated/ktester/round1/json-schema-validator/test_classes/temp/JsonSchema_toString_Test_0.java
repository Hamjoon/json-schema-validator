package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

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
        return Stream.of(// ---------- Standard Cases ----------
        Arguments.of("/properties/name", "{\"type\":\"string\"}", "\"/properties/name\" : {\"type\":\"string\"}"), Arguments.of("/items", "[{\"type\":\"string\"},{\"type\":\"number\"}]", "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]"), // ---------- Edge Cases ----------
        // empty path
        Arguments.// empty path
        of(// empty path
        "", "{\"type\":\"integer\"}", "\"\" : {\"type\":\"integer\"}"), // internal double‑quote
        Arguments.// internal double‑quote
        of(// internal double‑quote
        "prop\"Name", "{\"type\":\"boolean\"}", "\"prop\"Name\" : {\"type\":\"boolean\"}"), // null path
        Arguments.// null path
        of(// null path
        null, "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"), // ---------- Special Characters ----------
        Arguments.of("/unicode/π", "{\"type\":\"number\"}", "\"/unicode/π\" : {\"type\":\"number\"}"), Arguments.of("/backslash\\test", "{\"type\":\"null\"}", "\"/backslash\\test\" : {\"type\":\"null\"}"), Arguments.of("prop'Name", "{\"type\":\"boolean\"}", "\"prop'Name\" : {\"type\":\"boolean\"}"), // ---------- Whitespace and Control Characters ----------
        Arguments.of("   /whitespace   ", "{\"type\":\"string\"}", "\"   /whitespace   \" : {\"type\":\"string\"}"), Arguments.of("/new\nline", "{\"type\":\"boolean\"}", "\"/new\nline\" : {\"type\":\"boolean\"}"), Arguments.of("/tab\tchar", "{\"type\":\"integer\"}", "\"/tab\tchar\" : {\"type\":\"integer\"}"), // ---------- Empty Nodes ----------
        Arguments.of("/emptyObject", "{}", "\"/emptyObject\" : {}"), Arguments.of("/emptyArray", "[]", "\"/emptyArray\" : []"), // ---------- Primitive Nodes ----------
        Arguments.of("/textNode", "\"simple text\"", "\"/textNode\" : \"simple text\""), Arguments.of("/numericNode", "123", "\"/numericNode\" : 123"), Arguments.of("/booleanNode", "true", "\"/booleanNode\" : true"), Arguments.of("/nullNodeString", "null", "\"/nullNodeString\" : null"), // ---------- Long Path ----------
        Arguments.of(generateLongPath(), "{\"type\":\"string\"}", "\"" + generateLongPath() + "\" : {\"type\":\"string\"}"), // ---------- Literal \"null\" Path ----------
        Arguments.of("null", "{\"type\":\"string\"}", "\"null\" : {\"type\":\"string\"}"));
    }

    /**
     * Helper to generate a path longer than 100 characters.
     */
    private static String generateLongPath() {
        // 110 characters (including the leading slash)
        return "/" + "a".repeat(109);
    }

    @Test
    void toString_returnsExpectedRepresentation() {
        // GIVEN
        String schemaPath = "/properties/name";
        String nodeString = "{\"type\":\"string\"}";
        when(schemaNode.toString()).thenReturn(nodeString);
        // The URI argument may be null; using a dummy URI keeps the call realistic.
        URI dummyUri = URI.create("http://example.com/schema.json");
        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, dummyUri, schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(), "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
    }

    @Test
    void toString_handlesEmptySchemaNodeString() {
        // GIVEN
        String schemaPath = "#";
        String nodeString = "";
        when(schemaNode.toString()).thenReturn(nodeString);
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
        // explicitly null
        JsonNode node = null;
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaPath, URI.create("http://example.com/schema.json"), node, null, false);
        // WHEN / THEN
        assertThrows(NullPointerException.class, jsonSchema::toString, "Calling toString() with a null schema node should throw NPE");
    }
}
