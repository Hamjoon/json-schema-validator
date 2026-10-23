package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    ValidationContext // used by JsonSchema
    validationContext;

    @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    // ----- Shared test data -------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // the JSON schema used for all tests
    private JsonNode rootSchemaNode;

    // instance under test
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1. Build a simple schema JSON tree that also contains a few extra
        //    nodes used by the URL‑encoded tests.
        // {
        //   "properties": { "name": {"type":"string"}, "age":{"type":"integer"} },
        //   "definitions": { "address": {"type":"object"} },
        //   "encoded path": {"type":"string"},
        //   "components": { "schemas": { "User": {"type":"object"} } }
        // }
        String schemaJson = """
            {
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "definitions": {
                "address": { "type": "object" }
              },
              "encoded path": { "type": "string" },
              "components": {
                "schemas": {
                  "User": { "type": "object" }
                }
              }
            }
            """;
        rootSchemaNode = MAPPER.readTree(schemaJson);
        // 2. Stub the ValidationContext behaviour required by JsonSchema.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null);
        // 3. Create the JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(validationContext, "test-schema.json", new URI("http://example.com/test-schema.json"), rootSchemaNode, // no parent
        null, // do not suppress sub‑schema retrieval
        false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithHashSlashPointer_ReturnsTargetNode() {
        // "#/properties/name" -> should resolve to the node { "type":"string" }
        JsonNode result = jsonSchema.getRefSchemaNode("#/properties/name");
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isObject(), "Result should be an object node");
        assertEquals("string", result.get("type").asText(), "The 'type' field should be 'string'");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("/properties/age");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_DelegatesToMetaSchema() {
        // Prepare a node that the mocked meta‑schema will return.
        JsonNode addressNode = rootSchemaNode.at("/definitions/address");
        assertFalse(addressNode.isMissingNode(), "Address node must exist in the test schema");
        // Configure the mock: when getNodeByFragmentRef is called with the fragment
        // "#/definitions/address" and the root schema node, return the address node.
        when(metaSchema.getNodeByFragmentRef("#/definitions/address", rootSchemaNode)).thenReturn(addressNode);
        JsonNode result = jsonSchema.getRefSchemaNode("#/definitions/address");
        assertNotNull(result);
        assertEquals(addressNode, result);
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUnresolvableFragment_ReturnsMissingNodeOrFallback() {
        // meta‑schema returns null for an unknown fragment.
        when(metaSchema.getNodeByFragmentRef("#/unknown", rootSchemaNode)).thenReturn(null);
        // The method should not throw; it will invoke the private handleNullNode.
        JsonNode result = jsonSchema.getRefSchemaNode("#/unknown");
        assertNotNull(result, "Result should never be null even for unknown fragments");
        // The contract of handleNullNode (private) is to attempt another lookup.
        // If that lookup also fails, the original missing node is returned.
        // Therefore we accept either a missing node or any node (the test simply
        // ensures no exception and a non‑null return).
        // No further assertions are made because the internal fallback is private.
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_MissingNode_TriggersFallback() {
        // "/nonexistent" does not exist – node.at will return MissingNode.
        JsonNode result = jsonSchema.getRefSchemaNode("/nonexistent");
        assertNotNull(result, "Even for missing targets the method must return a non‑null node");
        // The exact fallback node is implementation‑specific; we only assert that
        // the result is not the original MissingNode (i.e. fallback was attempted).
        assertFalse(result.isMissingNode(), "Fallback should produce a non‑missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_ResolvedByMetaSchema() {
        // Prepare a dummy node to be returned by the mocked meta‑schema.
        ObjectNode dummy = MAPPER.createObjectNode().put("dummy", true);
        when(metaSchema.getNodeByFragmentRef("#foo", rootSchemaNode)).thenReturn(dummy);
        JsonNode result = jsonSchema.getRefSchemaNode("#foo");
        assertNotNull(result);
        assertSame(dummy, result, "The node returned by the meta‑schema should be forwarded");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrnFragment_Unresolvable_UsesFallback() {
        when(metaSchema.getNodeByFragmentRef("urn:example:schema", rootSchemaNode)).thenReturn(null);
        JsonNode result = jsonSchema.getRefSchemaNode("urn:example:schema");
        assertNotNull(result, "Method must return a non‑null node even when meta‑schema cannot resolve");
        assertFalse(result.isMissingNode(), "Fallback should provide a non‑missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithPlainReference_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("simple");
        assertSame(rootSchemaNode, result, "Plain references should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrlEncodedPointer_ReturnsDecodedNode() {
        // The schema contains a property named "encoded path".
        JsonNode result = jsonSchema.getRefSchemaNode("#/encoded%20path");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText(), "URL‑decoded pointer should resolve to the correct node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_ReferenceHashOnly_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#");
        assertSame(rootSchemaNode, result, "Reference \"#\" should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_EmptyString_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("");
        assertSame(rootSchemaNode, result, "Empty reference should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_UrnFourChars_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("urn:");
        assertSame(rootSchemaNode, result, "\"urn:\" is too short for the fragment branch and should return the original node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_HashSlashMissingNode_Fallback() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/nonexistent");
        assertNotNull(result);
        assertFalse(result.isMissingNode(), "Fallback should replace the missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithEncodedSlashInsidePath() {
        // After decoding "%2F" becomes "/", so the pointer resolves to
        // "/components/schemas/User".
        JsonNode result = jsonSchema.getRefSchemaNode("#/components%2Fschemas%2FUser");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText(), "Decoded pointer should resolve to the User schema object");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> jsonSchema.getRefSchemaNode(null));
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_FindAncestorNull_ThrowsNPE() throws Exception {
        // Spy on the instance and force findAncestor() to return null.
        JsonSchema spy = spy(jsonSchema);
        doReturn(null).when(spy).findAncestor();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("#/properties/name"), "When findAncestor() returns null a NPE should be thrown");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_AncestorSchemaNodeNull_ThrowsNPE() {
        // Mock an ancestor schema whose getSchemaNode() returns null.
        JsonSchema ancestorMock = mock(JsonSchema.class);
        when(ancestorMock.getSchemaNode()).thenReturn(null);
        JsonSchema spy = spy(jsonSchema);
        doReturn(ancestorMock).when(spy).findAncestor();
        // Both a "#/…" and a plain "/…" reference should hit the node.at(...)
        assertAll(() -> assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("#/properties/name"), "Null schema node should cause NPE for \"#/…\" reference"), () -> assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/properties/name"), "Null schema node should cause NPE for \"/…\" reference"));
    }
}
