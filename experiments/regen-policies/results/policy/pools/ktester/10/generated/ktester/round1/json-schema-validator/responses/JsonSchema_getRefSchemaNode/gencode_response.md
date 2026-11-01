```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#getRefSchemaNode(String)}.
 *
 * The tests cover all scenarios described in the supplied JSON test‑case list,
 * including handling of schema $id prefixes, JSON‑pointer syntax, URL‑decoding,
 * missing nodes (fallback to {@code handleNullNode}), and error conditions.
 *
 * Dependencies such as {@link JsonSchema#findSchemaResourceRoot()},
 * {@link JsonSchema#getSchemaNode()} and {@link JsonSchema#getId()} are mocked.
 *
 * The private method {@code handleNullNode} is stubbed via a Mockito spy
 * (requires mockito‑inline on the classpath).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    /** Shared JSON document used by most tests. */
    private static JsonNode TEST_ROOT_NODE;

    /** ObjectMapper for building the test JSON tree. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void initRootNode() throws Exception {
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

    /** --------------------------------------------------------------------- **/
    /** --------------------------- SUCCESS CASES --------------------------- **/
    /** --------------------------------------------------------------------- **/

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

    /** --------------------------------------------------------------------- **/
    /** ---------------------------- ERROR CASES --------------------------- **/
    /** --------------------------------------------------------------------- **/

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
```