package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The schema under test.
     */
    private JsonSchema schema;

    /**
     * Sample schema node used by the tests.
     */
    private JsonNode schemaNode;

    /**
     * Schema without a $id – used for most test cases.
     */
    private JsonSchema schemaWithoutId;

    /**
     * Schema that contains a $id – used for id‑related test cases.
     */
    private JsonSchema schemaWithId;

    /**
     * Sample JSON document that contains the nodes referenced by the tests.
     */
    private static final String JSON_WITH_ID = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "http://example.com/schemas/main",
          "components": {
            "schemas": {
              "Person": {
                "type": "object",
                "properties": {}
              }
            }
          },
          "defs": {
            "thing": {
              "type": "string"
            }
          },
          "definitions": {
            "foo": {
              "type": "string",
              "description": "A simple string"
            },
            "bar": {
              "type": "integer"
            }
          }
        }
        """;

    /**
     * Same document but **without** the $id property.
     */
    private static final String JSON_WITHOUT_ID = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "components": {
            "schemas": {
              "Person": {
                "type": "object",
                "properties": {}
              }
            }
          },
          "defs": {
            "thing": {
              "type": "string"
            }
          },
          "definitions": {
            "foo": {
              "type": "string",
              "description": "A simple string"
            },
            "bar": {
              "type": "integer"
            }
          }
        }
        """;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Prepare a minimal JSON schema document.
        // -----------------------------------------------------------------
        String json = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "definitions": {
                "foo": {
                  "type": "string",
                  "description": "A simple string"
                },
                "bar": {
                  "type": "integer"
                }
              }
            }
            """;
        schemaNode = MAPPER.readTree(json);
        // -----------------------------------------------------------------
        // 2. Mock the heavy collaborators required by the JsonSchema ctor.
        // -----------------------------------------------------------------
        ValidationContext validationContext = mock(ValidationContext.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        // root schema – no parent.
        JsonSchema parent = null;
        // ValidationContext stubs
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // JsonMetaSchema stubs – no anchors in our tiny schema.
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
        // SchemaLocation stub – we do not need a real IRI.
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        // -----------------------------------------------------------------
        // 3. Create the JsonSchema instance via the package‑private factory.
        // -----------------------------------------------------------------
        schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
    }

    /**
     * Provides all normal test scenarios described in the JSON test‑case list.
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the {@link JsonSchema} instance to use (with/without $id)</li>
     *   <li>the {@code ref} argument</li>
     *   <li>the expected JSON‑Pointer path relative to the root node (or {@code null}
     *       when the original root node is expected)</li>
     *   <li>boolean flag – {@code true} if the method is expected to return a
     *       {@link com.fasterxml.jackson.databind.node.MissingNode}</li>
     *   <li>a description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> provideRefScenarios() {
        return Stream.of(// 1. simpleRef – no id, no special prefix → original schema node
        arguments("schemaWithoutId", "simpleRef", null, false, "original schema node"), // 2. id‑aware ref with "#"
        arguments("schemaWithId", "http://example.com/schemas/main#/defs/thing", "/defs/thing", false, "node at path \"/defs/thing\" (id stripped, hash present)"), // 3. id‑aware ref without "#"
        arguments("schemaWithId", "http://example.com/schemas/main/defs/thing", "/defs/thing", false, "node at path \"/defs/thing\" (id stripped, no hash)"), // 4. standard JSON‑Pointer with leading "#/"
        arguments("schemaWithoutId", "#/components/schemas/Person", "/components/schemas/Person", false, "node at path \"/components/schemas/Person\""), // 5. missing path → fallback (MissingNode)
        arguments("schemaWithoutId", "/missing/path", null, true, "node returned by handleNullNode (missing)"), // 6. ref exactly equals schema id → original node
        arguments("schemaWithId", "http://example.com/schemas/main", null, false, "original schema node (id matches exactly)"), // 7. hash without slash → original node
        arguments("schemaWithoutId", "#plainFragment", null, false, "original schema node (hash without slash)"), // 8. hash followed only by slash → original node (root)
        arguments("schemaWithoutId", "#/", null, false, "original schema node (hash followed by slash)"), // 9. absolute pointer without hash
        arguments("schemaWithoutId", "/components/schemas/Person", "/components/schemas/Person", false, "node at path \"/components/schemas/Person\""), //10. URL‑encoded slash after hash
        arguments("schemaWithoutId", "#/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //11. URL‑encoded slash without hash
        arguments("schemaWithoutId", "/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //12. empty string → original node
        arguments("schemaWithoutId", "", null, false, "original schema node (empty ref)"), //13. solitary "#" → original node
        arguments("schemaWithoutId", "#", null, false, "original schema node (sole '#')"), //14. ref with non‑matching id prefix → original node
        arguments("schemaWithId", "http://example.com/schemas/mainExtra#/defs/thing", null, false, "original schema node (id does not match)"), //15. missing node after hash → fallback (MissingNode)
        arguments("schemaWithoutId", "#/missing/node", null, true, "node returned by handleNullNode (missing)"));
    }

    /**
     * Verify that a valid JSON‑Pointer (starting with {@code "#/"} ) is resolved
     * correctly to the target node.
     */
    @Test
    void testGetRefSchemaNode_ValidPointer() {
        // "#/definitions/foo" points to the object { "type":"string", ... }
        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Result should not be a missing node");
        assertEquals("string", result.get("type").asText(), "The resolved node should contain the expected type");
        assertEquals("A simple string", result.get("description").asText(), "The resolved node should contain the expected description");
    }

    /**
     * Verify that a pointer without the leading {@code "#"} is also handled
     * (the method strips the leading {@code "#"} internally).
     */
    @Test
    void testGetRefSchemaNode_PointerWithoutHash() {
        // The method internally removes a leading "#" – we pass "/definitions/bar".
        JsonNode result = schema.getRefSchemaNode("/definitions/bar");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("integer", result.get("type").asText(), "Should resolve to the integer definition");
    }

    /**
     * Verify that an unknown pointer results in a missing node and that the
     * private {@code handleNullNode} fallback is exercised (it returns a missing
     * node in our minimal setup).
     */
    @Test
    void testGetRefSchemaNode_UnknownPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/unknown");
        // In our test configuration {@code handleNullNode} ends up returning a
        // missing node because there is no sub‑schema to follow.
        assertTrue(result.isMissingNode(), "An unknown pointer should yield a missing node after fallback");
    }

    /**
     * Verify that URL‑encoded pointers are decoded before navigation.
     */
    @Test
    void testGetRefSchemaNode_UrlEncodedPointer() {
        // Encode the '/' character: "%2Fdefinitions%2Ffoo"
        JsonNode result = schema.getRefSchemaNode("#%2Fdefinitions%2Ffoo");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("string", result.get("type").asText(), "URL‑decoded pointer should resolve correctly");
    }

    /**
     * Executes the parameterised scenarios defined in {@link #provideRefScenarios()}.
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideRefScenarios")
    void testGetRefSchemaNode_VariousScenarios(String schemaKey, String ref, String expectedPath, boolean expectMissing, String description) throws Exception {
        JsonSchema schema = "schemaWithId".equals(schemaKey) ? schemaWithId : schemaWithoutId;
        JsonNode result = schema.getRefSchemaNode(ref);
        if (expectMissing) {
            assertTrue(result.isMissingNode(), "Expected a MissingNode for ref \"" + ref + "\"");
            return;
        }
        if (expectedPath == null) {
            // Expect the original root node.
            assertSame(schema.getSchemaNode(), result, "Expected the original schema node for ref \"" + ref + "\"");
        } else {
            JsonNode expected = schema.getSchemaNode().at(expectedPath);
            assertEquals(expected, result, "Resolved node does not match expected node for ref \"" + ref + "\"");
        }
    }

    @Test
    void testGetRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> schemaWithoutId.getRefSchemaNode(null), "Passing null should raise NullPointerException");
    }

    @Test
    void testGetRefSchemaNode_FindSchemaResourceRootReturnsNull_ThrowsNPE() {
        JsonSchema spy = spy(schemaWithoutId);
        // Stub the internal call to return null.
        doReturn(null).when(spy).findSchemaResourceRoot();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/path"), "When findSchemaResourceRoot() returns null a NPE should be thrown");
    }

    @Test
    void testGetRefSchemaNode_RootNodeIsNull_ThrowsNPE() {
        JsonSchema spy = spy(schemaWithoutId);
        // Stub the root node to be null.
        doReturn(null).when(spy).getSchemaNode();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/path"), "When the root schema node is null a NPE should be thrown");
    }

    @Test
    void testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException() {
        // The "~" character is illegal in a JSON‑Pointer unless escaped.
        assertThrows(IllegalArgumentException.class, () -> schemaWithoutId.getRefSchemaNode("/invalid~pointer"), "An illegal JSON‑Pointer should propagate IllegalArgumentException");
    }
}
