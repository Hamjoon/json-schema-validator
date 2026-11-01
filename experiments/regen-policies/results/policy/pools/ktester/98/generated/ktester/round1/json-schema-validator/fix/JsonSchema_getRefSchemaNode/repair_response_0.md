```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#getRefSchemaNode(String)}.
 *
 * <p>The tests create real {@link JsonSchema} instances via {@link JsonSchemaFactory}
 * instead of using Mockito, so they can be compiled and run without any external
 * mocking framework.</p>
 */
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Schema without a $id – used for most test cases. */
    private JsonSchema schemaWithoutId;

    /** Schema that contains a $id – used for id‑related test cases. */
    private JsonSchema schemaWithId;

    /** The schema used by the simple pointer tests. */
    private JsonSchema schema;

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

    /**
     * Creates three {@link JsonSchema} instances:
     * <ul>
     *   <li>a minimal schema used by the first few tests</li>
     *   <li>a schema without an {@code $id}</li>
     *   <li>a schema with an {@code $id}</li>
     * </ul>
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Minimal schema (used by the non‑parameterised tests)
        // -----------------------------------------------------------------
        String minimal = """
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
        JsonNode minimalNode = MAPPER.readTree(minimal);
        schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(minimalNode);

        // -----------------------------------------------------------------
        // 2. Schema without $id
        // -----------------------------------------------------------------
        JsonNode withoutIdNode = MAPPER.readTree(JSON_WITHOUT_ID);
        schemaWithoutId = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(withoutIdNode);

        // -----------------------------------------------------------------
        // 3. Schema with $id
        // -----------------------------------------------------------------
        JsonNode withIdNode = MAPPER.readTree(JSON_WITH_ID);
        schemaWithId = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(withIdNode);
    }

    /**
     * Provides all normal test scenarios described in the JSON test‑case list.
     */
    private static Stream<Arguments> provideRefScenarios() {
        return Stream.of(
                // 1. simpleRef – no id, no special prefix → original schema node
                arguments("schemaWithoutId", "simpleRef", null, false, "original schema node"),
                // 2. id‑aware ref with "#"
                arguments("schemaWithId", "http://example.com/schemas/main#/defs/thing", "/defs/thing", false,
                        "node at path \"/defs/thing\" (id stripped, hash present)"),
                // 3. id‑aware ref without "#"
                arguments("schemaWithId", "http://example.com/schemas/main/defs/thing", "/defs/thing", false,
                        "node at path \"/defs/thing\" (id stripped, no hash)"),
                // 4. standard JSON‑Pointer with leading "#/"
                arguments("schemaWithoutId", "#/components/schemas/Person", "/components/schemas/Person", false,
                        "node at path \"/components/schemas/Person\""),
                // 5. missing path → fallback (MissingNode)
                arguments("schemaWithoutId", "/missing/path", null, true,
                        "node returned by handleNullNode (missing)"),
                // 6. ref exactly equals schema id → original node
                arguments("schemaWithoutId", "http://example.com/schemas/main", null, false,
                        "original schema node (id matches exactly)"),
                // 7. id matches exactly (with $id)
                arguments("schemaWithId", "http://example.com/schemas/main", null, false,
                        "original schema node (id matches exactly)"),
                // 8. hash without slash → original node
                arguments("schemaWithoutId", "#plainFragment", null, false,
                        "original schema node (hash without slash)"),
                // 9. hash followed only by slash → original node (root)
                arguments("schemaWithoutId", "#/", null, false,
                        "original schema node (hash followed by slash)"),
                //10. absolute pointer without hash
                arguments("schemaWithoutId", "/components/schemas/Person", "/components/schemas/Person", false,
                        "node at path \"/components/schemas/Person\""),
                //11. URL‑encoded slash after hash
                arguments("schemaWithoutId", "#/defs%2Fthing", "/defs/thing", false,
                        "node at path \"/defs/thing\" (URL‑encoded)"),
                //12. URL‑encoded slash without hash
                arguments("schemaWithoutId", "/defs%2Fthing", "/defs/thing", false,
                        "node at path \"/defs/thing\" (URL‑encoded)"),
                //13. empty string → original node
                arguments("schemaWithoutId", "", null, false,
                        "original schema node (empty ref)"),
                //14. solitary "#" → original node
                arguments("schemaWithoutId", "#", null, false,
                        "original schema node (sole '#')"),
                //15. ref with non‑matching id prefix → original node
                arguments("schemaWithId", "http://example.com/schemas/mainExtra#/defs/thing", null, false,
                        "original schema node (id does not match)"),
                //16. missing node after hash → fallback (MissingNode)
                arguments("schemaWithoutId", "#/missing/node", null, true,
                        "node returned by handleNullNode (missing)")
        );
    }

    /** Verify that a valid JSON‑Pointer (starting with {@code "#/"} ) is resolved correctly. */
    @Test
    void testGetRefSchemaNode_ValidPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Result should not be a missing node");
        assertEquals("string", result.get("type").asText(), "The resolved node should contain the expected type");
        assertEquals("A simple string", result.get("description").asText(),
                "The resolved node should contain the expected description");
    }

    /** Verify that a pointer without the leading {@code "#"} is also handled. */
    @Test
    void testGetRefSchemaNode_PointerWithoutHash() {
        JsonNode result = schema.getRefSchemaNode("/definitions/bar");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("integer", result.get("type").asText(), "Should resolve to the integer definition");
    }

    /** Verify that an unknown pointer results in a missing node. */
    @Test
    void testGetRefSchemaNode_UnknownPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/unknown");
        assertTrue(result.isMissingNode(), "An unknown pointer should yield a missing node after fallback");
    }

    /** Verify that URL‑encoded pointers are decoded before navigation. */
    @Test
    void testGetRefSchemaNode_UrlEncodedPointer() {
        JsonNode result = schema.getRefSchemaNode("#%2Fdefinitions%2Ffoo");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("string", result.get("type").asText(), "URL‑decoded pointer should resolve correctly");
    }

    /** Executes the parameterised scenarios defined in {@link #provideRefScenarios()}. */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideRefScenarios")
    void testGetRefSchemaNode_VariousScenarios(String schemaKey, String ref, String expectedPath,
                                                boolean expectMissing, String description) {
        JsonSchema target = "schemaWithId".equals(schemaKey) ? schemaWithId : schemaWithoutId;
        JsonNode result = target.getRefSchemaNode(ref);
        if (expectMissing) {
            assertTrue(result.isMissingNode(),
                    "Expected a MissingNode for ref \"" + ref + "\"");
            return;
        }
        if (expectedPath == null) {
            // Expect the original root node.
            assertSame(target.getSchemaNode(), result,
                    "Expected the original schema node for ref \"" + ref + "\"");
        } else {
            JsonNode expected = target.getSchemaNode().at(expectedPath);
            assertEquals(expected, result,
                    "Resolved node does not match expected node for ref \"" + ref + "\"");
        }
    }

    /** Passing {@code null} should raise {@link NullPointerException}. */
    @Test
    void testGetRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> schemaWithoutId.getRefSchemaNode(null),
                "Passing null should raise NullPointerException");
    }

    /** An illegal JSON‑Pointer should propagate {@link IllegalArgumentException}. */
    @Test
    void testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException() {
        // The "~" character is illegal in a JSON‑Pointer unless escaped.
        assertThrows(IllegalArgumentException.class,
                () -> schemaWithoutId.getRefSchemaNode("/invalid~pointer"),
                "An illegal JSON‑Pointer should propagate IllegalArgumentException");
    }
}
```