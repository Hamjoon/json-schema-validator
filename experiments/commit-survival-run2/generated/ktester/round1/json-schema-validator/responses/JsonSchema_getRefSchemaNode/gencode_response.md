```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#getRefSchemaNode(String)}.
 *
 * <p>Each test case corresponds to one of the scenarios described in the
 * specification JSON.  The tests use a real {@link JsonSchema} instance created
 * via the package‑private factory method and mock the {@link ValidationContext}
 * and {@link JsonMetaSchema} collaborators where required.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    ValidationContext validationContext;          // used by JsonSchema
    @Mock
    JsonMetaSchema metaSchema;                    // returned by validationContext.getMetaSchema()

    // ----- Shared test data -------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonNode rootSchemaNode;            // the JSON schema used for most tests
    private JsonSchema jsonSchema;              // instance under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

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
        jsonSchema = JsonSchema.from(
                validationContext,
                "test-schema.json",
                new URI("http://example.com/test-schema.json"),
                rootSchemaNode,
                null,                     // no parent
                false                     // do not suppress sub‑schema retrieval
        );
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // ------------------------------------------------------------------------
    // 1. JSON Pointer starting with "#/" – normal (non‑missing) node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithHashSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/properties/name");
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isObject(), "Result should be an object node");
        assertEquals("string", result.get("type").asText(), "The 'type' field should be 'string'");
    }

    // ------------------------------------------------------------------------
    // 2. JSON Pointer starting with "/" – normal (non‑missing) node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("/properties/age");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    // ------------------------------------------------------------------------
    // 3. JSON Pointer starting with "/" – missing node triggers fallback.
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
    // 4. Fragment reference starting with "#" (non‑empty) – meta‑schema resolves.
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
    // 5. Fragment reference starting with "urn:" – meta‑schema cannot resolve,
    //    fallback is used.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrnFragment_Unresolvable_UsesFallback() {
        when(metaSchema.getNodeByFragmentRef("urn:example:schema", rootSchemaNode)).thenReturn(null);

        JsonNode result = jsonSchema.getRefSchemaNode("urn:example:schema");
        assertNotNull(result, "Method must return a non‑null node even when meta‑schema cannot resolve");
        assertFalse(result.isMissingNode(), "Fallback should provide a non‑missing node");
    }

    // ------------------------------------------------------------------------
    // 6. Reference with no special prefix – returns the original schema node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithPlainReference_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("simple");
        assertSame(rootSchemaNode, result, "Plain references should return the original schema node");
    }

    // ------------------------------------------------------------------------
    // 7. JSON Pointer with URL‑encoded characters.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrlEncodedPointer_ReturnsDecodedNode() {
        // The schema contains a property named "encoded path".
        JsonNode result = jsonSchema.getRefSchemaNode("#/encoded%20path");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText(),
                "URL‑decoded pointer should resolve to the correct node");
    }

    // ------------------------------------------------------------------------
    // 8. Reference equal to "#" – returns original schema node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_ReferenceHashOnly_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#");
        assertSame(rootSchemaNode, result, "Reference \"#\" should return the original schema node");
    }

    // ------------------------------------------------------------------------
    // 9. Empty string reference – returns original schema node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_EmptyString_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("");
        assertSame(rootSchemaNode, result, "Empty reference should return the original schema node");
    }

    // ------------------------------------------------------------------------
    // 10. Reference starting with "urn:" but exactly 4 characters – falls back
    //     to original node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_UrnFourChars_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("urn:");
        assertSame(rootSchemaNode, result,
                "\"urn:\" is too short for the fragment branch and should return the original node");
    }

    // ------------------------------------------------------------------------
    // 11. JSON Pointer with "#/" that resolves to a missing node – fallback.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_HashSlashMissingNode_Fallback() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/nonexistent");
        assertNotNull(result);
        assertFalse(result.isMissingNode(), "Fallback should replace the missing node");
    }

    // ------------------------------------------------------------------------
    // 12. JSON Pointer with URL‑encoded slash (%2F) inside the path.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithEncodedSlashInsidePath() {
        // After decoding "%2F" becomes "/", so the pointer resolves to
        // "/components/schemas/User".
        JsonNode result = jsonSchema.getRefSchemaNode("#/components%2Fschemas%2FUser");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText(),
                "Decoded pointer should resolve to the User schema object");
    }

    // ------------------------------------------------------------------------
    // 13. Null reference argument – NullPointerException.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> jsonSchema.getRefSchemaNode(null));
    }

    // ------------------------------------------------------------------------
    // 14. findAncestor returns null – NullPointerException.
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
    // 15. schema.getSchemaNode returns null – NullPointerException on node.at().
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

    // ------------------------------------------------------------------------
    // 16. metaSchema is null – NullPointerException on fragment resolution.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_MetaSchemaNull_ThrowsNPE() throws Exception {
        // Set the private metaSchema field to null via reflection.
        Field metaField = JsonSchema.class.getDeclaredField("metaSchema");
        metaField.setAccessible(true);
        metaField.set(jsonSchema, null);

        // Both a "#" fragment and a "urn:"