package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    /**
     * Shared JSON document used by all tests.
     */
    private static JsonNode TEST_ROOT_NODE;

    /**
     * ObjectMapper for building the test JSON tree.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // {"a":{"b":1},"a/b":2}
        String json = """
            {
              "a": {
                "b": 1
              },
              "a/b": 2
            }
            """;
        TEST_ROOT_NODE = MAPPER.readTree(json);
        /*
         * The test schema contains a variety of structures required by the
         * different test cases:
         *
         *   - /properties/name   -> {"type":"string"}
         *   - /properties/age    -> {"type":"integer"}
         *   - /definitions/person -> {"type":"object"}
         *   - /properties/first name -> {"type":"string"}
         *   - /items/0            -> {"type":"string"}
         *   - /properties/address/street -> {"type":"string"}
         *   - /definitions/emptyId -> {"type":"object"}
         */
        ObjectNode root = MAPPER.createObjectNode();
        // /properties
        ObjectNode properties = root.putObject("properties");
        properties.set("name", nodeOf("{\"type\":\"string\"}"));
        properties.set("age", nodeOf("{\"type\":\"integer\"}"));
        properties.set("first name", nodeOf("{\"type\":\"string\"}"));
        // nested address object
        ObjectNode address = properties.putObject("address");
        address.set("street", nodeOf("{\"type\":\"string\"}"));
        // /definitions
        ObjectNode definitions = root.putObject("definitions");
        definitions.set("person", nodeOf("{\"type\":\"object\"}"));
        definitions.set("emptyId", nodeOf("{\"type\":\"object\"}"));
        // /items array
        ArrayNode items = root.putArray("items");
        items.add(nodeOf("{\"type\":\"string\"}"));
        TEST_ROOT_NODE = root;
    }

    /**
     * Helper that parses a tiny JSON snippet into a {@link JsonNode}.
     */
    private static JsonNode nodeOf(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simple reference that starts with “#/”. The method should resolve the
     * pointer and return the node located at /a/b (value 1).
     */
    @Test
    void shouldResolveSimpleJsonPointer() {
        // Arrange ------------------------------------------------------------
        // Mock the root schema that holds the JSON document.
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        // no $id for this test
        when(rootSchema.getId()).thenReturn(null);
        // The schema under test delegates to the root schema.
        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);
        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode("#/a/b");
        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(1, result.intValue(), "The value at /a/b should be 1");
    }

    /**
     * Reference that contains the schema’s $id as a prefix.
     * The method must strip the id, then resolve the remaining pointer.
     */
    @Test
    void shouldStripSchemaIdPrefixWhenPresent() {
        // Arrange ------------------------------------------------------------
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/root");
        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);
        // The reference contains the id followed by the JSON‑pointer.
        String ref = "http://example.com/root#/a/b";
        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode(ref);
        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(1, result.intValue(), "The value at /a/b should be 1");
    }

    /**
     * Reference that uses URL‑encoded characters (e.g. “%2F” for “/”).
     * The method must URL‑decode the pointer before calling {@code at()}.
     */
    @Test
    void shouldDecodeUrlEncodedPointer() {
        // Arrange ------------------------------------------------------------
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);
        // The key in the JSON document is "a/b". The pointer must be encoded.
        String ref = "#/a%2Fb";
        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode(ref);
        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(2, result.intValue(), "The value at /a/b (encoded) should be 2");
    }

    @Test
    void noIdSimpleAbsoluteJsonPointer() {
        // reference: "/properties/name"
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    void schemaIdPresentReferenceStartsWithId() {
        // schema id + fragment
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/schema");
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("http://example.com/schema#/properties/age");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    @Test
    void referenceStartsWithFragmentOnly() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("#/definitions/person");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText());
    }

    @Test
    void urlDecodedPointerSpace() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("/properties/first%20name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    void missingNodeFallsBackToHandleNullNode() throws Exception {
        // Build a fallback node that we expect to be returned.
        JsonNode fallback = nodeOf("{\"fallback\":true}");
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        // Spy the schema under test so we can stub the private method.
        JsonSchema schemaSpy = spy(JsonSchema.class);
        when(schemaSpy.findSchemaResourceRoot()).thenReturn(rootSchema);
        // Stub private handleNullNode via Mockito's inline mock maker.
        doReturn(fallback).when(schemaSpy, "handleNullNode", "/nonexistent/path", rootSchema);
        JsonNode result = schemaSpy.getRefSchemaNode("/nonexistent/path");
        assertSame(fallback, result, "Should return the node supplied by handleNullNode");
    }

    @Test
    void referenceDoesNotStartWithSlashOrHashReturnsRootNode() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("plainReference");
        assertSame(TEST_ROOT_NODE, result);
    }

    @Test
    void idPresentReferenceStartsWithIdNoFragment() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/schema");
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("http://example.com/schema/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    void idPresentReferenceStartsWithIdButNoLeadingSlashAfterId() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/schema");
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("http://example.com/schemaproperties/name");
        // No '/' after stripping the id, therefore method returns the root node.
        assertSame(TEST_ROOT_NODE, result);
    }

    @Test
    void referenceEqualsSchemaIdExactlyReturnsRootNode() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/schema");
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("http://example.com/schema");
        assertSame(TEST_ROOT_NODE, result);
    }

    @Test
    void emptyReferenceStringReturnsRootNode() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("");
        assertSame(TEST_ROOT_NODE, result);
    }

    @Test
    void referenceIsJustHashReturnsRootNode() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("#");
        assertSame(TEST_ROOT_NODE, result);
    }

    @Test
    void arrayIndexJsonPointer() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("/items/0");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    void encodedSlashInPointer() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("/properties%2Faddress/street");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    void schemaIdIsEmptyStringTreatedAsNoId() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("");
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        JsonNode result = schema.getRefSchemaNode("/definitions/emptyId");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText());
    }

    @Test
    void nullReferenceArgumentThrowsNPE() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        assertThrows(NullPointerException.class, () -> schema.getRefSchemaNode(null));
    }

    @Test
    void schemaResourceRootIsNullThrowsNPE() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schema.getRefSchemaNode("/properties/name"));
    }

    @Test
    void schemaNodeIsNullThrowsNPE() {
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(null);
        when(rootSchema.getId()).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.findSchemaResourceRoot()).thenReturn(rootSchema);
        // The method will attempt to call node.at(...), which triggers NPE.
        assertThrows(NullPointerException.class, () -> schema.getRefSchemaNode("/properties/name"));
    }
}
