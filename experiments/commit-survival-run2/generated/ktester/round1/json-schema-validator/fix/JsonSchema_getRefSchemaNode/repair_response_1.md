```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    ValidationContext validationContext; // used by JsonSchema

    @Mock
    JsonMetaSchema metaSchema; // returned by validationContext.getMetaSchema()

    // ----- Shared test data -------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // the JSON schema used for all tests
    private JsonNode rootSchemaNode;

    // instance under test
    private JsonSchema jsonSchema;

    /**
     * Very small stub of {@link JsonMetaSchema} that allows us to
     * control the result of {@code getNodeByFragmentRef}.
     */
    private static class SimpleJsonMetaSchema extends JsonMetaSchema {

        private final Map<String, JsonNode> overrides = new HashMap<>();

        SimpleJsonMetaSchema() {
            // The constructor of JsonMetaSchema expects an id and a URI.
            // The concrete values are irrelevant for these tests.
            super("simple", URI.create("http://example.com/simple"), false, false);
        }

        /** Register a node that should be returned for a given fragment. */
        void addOverride(String fragment, JsonNode node) {
            overrides.put(fragment, node);
        }

        @Override
        public JsonNode getNodeByFragmentRef(String fragment, JsonNode rootNode) {
            // First check if a test has explicitly registered an override.
            if (overrides.containsKey(fragment)) {
                return overrides.get(fragment);
            }
            // Default behaviour – try to resolve via standard JSON Pointer.
            JsonNode candidate = rootNode.at(fragment);
            return candidate.isMissingNode() ? null : candidate;
        }

        // The remaining methods of JsonMetaSchema are not needed for these tests.
        @Override public String getId() { return null; }

        @Override public String getUri() { return null; }

        @Override public boolean isFormat(String format) { return false; }

        @Override public boolean isKeyword(String keyword) { return false; }

        @Override public boolean isKeywordSupported(String keyword) { return false; }

        @Override public boolean isKeywordSupported(String keyword, boolean strict) { return false; }

        @Override public boolean isFormatSupported(String format) { return false; }

        @Override public boolean isFormatSupported(String format, boolean strict) { return false; }

        @Override public boolean isCustomFormat(String format) { return false; }

        @Override public boolean isCustomKeyword(String keyword) { return false; }

        @Override public boolean isCustomKeyword(String keyword, boolean strict) { return false; }

        @Override public String getFormat(String format) { return null; }

        @Override public String getKeyword(String keyword) { return null; }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
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

        // Create a concrete stub for JsonMetaSchema
        metaSchema = new SimpleJsonMetaSchema();

        // Configure the ValidationContext mock to return our stub meta‑schema
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);

        // Build the JsonSchema instance (package‑private factory method)
        jsonSchema = JsonSchema.from(validationContext,
                "test-schema.json",
                new URI("http://example.com/test-schema.json"),
                rootSchemaNode,
                null,
                false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithHashSlashPointer_ReturnsTargetNode() {
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
        JsonNode addressNode = rootSchemaNode.at("/definitions/address");
        assertFalse(addressNode.isMissingNode(), "Address node must exist in the test schema");
        // Register the expected fragment result in the meta‑schema stub.
        ((SimpleJsonMetaSchema) metaSchema).addOverride("#/definitions/address", addressNode);
        JsonNode result = jsonSchema.getRefSchemaNode("#/definitions/address");
        assertNotNull(result);
        assertEquals(addressNode, result);
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUnresolvableFragment_ReturnsMissingNodeOrFallback() {
        // No override is added – the stub will return null for this fragment.
        JsonNode result = jsonSchema.getRefSchemaNode("#/unknown");
        assertNotNull(result, "Result should never be null even for unknown fragments");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_MissingNode_TriggersFallback() {
        // "/nonexistent" does not exist – node.at will return MissingNode.
        JsonNode result = jsonSchema.getRefSchemaNode("/nonexistent");
        assertNotNull(result, "Even for missing targets the method must return a non‑null node");
        assertFalse(result.isMissingNode(), "Fallback should produce a non‑missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_ResolvedByMetaSchema() {
        ObjectNode dummy = MAPPER.createObjectNode().put("dummy", true);
        ((SimpleJsonMetaSchema) metaSchema).addOverride("#foo", dummy);
        JsonNode result = jsonSchema.getRefSchemaNode("#foo");
        assertNotNull(result);
        assertSame(dummy, result, "The node returned by the meta‑schema should be forwarded");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrnFragment_Unresolvable_UsesFallback() {
        // No override for this URN – stub will return null.
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
        assertThrows(NullPointerException.class,
                () -> spy.getRefSchemaNode("#/properties/name"),
                "When findAncestor() returns null a NPE should be thrown");
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
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> spy.getRefSchemaNode("#/properties/name"),
                        "Null schema node should cause NPE for \"#/…\" reference"),
                () -> assertThrows(NullPointerException.class,
                        () -> spy.getRefSchemaNode("/properties/name"),
                        "Null schema node should cause NPE for \"/…\" reference")
        );
    }
}
```