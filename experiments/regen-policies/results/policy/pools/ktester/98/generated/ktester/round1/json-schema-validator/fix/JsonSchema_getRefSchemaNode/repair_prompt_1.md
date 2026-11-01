
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

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
        arguments("schemaWithoutId", "http://example.com/schemas/main", null, false, "original schema node (id matches exactly)"), // 7. id matches exactly (with $id)
        arguments("schemaWithId", "http://example.com/schemas/main", null, false, "original schema node (id matches exactly)"), // 8. hash without slash → original node
        arguments("schemaWithoutId", "#plainFragment", null, false, "original schema node (hash without slash)"), // 9. hash followed only by slash → original node (root)
        arguments("schemaWithoutId", "#/", null, false, "original schema node (hash followed by slash)"), //10. absolute pointer without hash
        arguments("schemaWithoutId", "/components/schemas/Person", "/components/schemas/Person", false, "node at path \"/components/schemas/Person\""), //11. URL‑encoded slash after hash
        arguments("schemaWithoutId", "#/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //12. URL‑encoded slash without hash
        arguments("schemaWithoutId", "/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //13. empty string → original node
        arguments("schemaWithoutId", "", null, false, "original schema node (empty ref)"), //14. solitary "#" → original node
        arguments("schemaWithoutId", "#", null, false, "original schema node (sole '#')"), //15. ref with non‑matching id prefix → original node
        arguments("schemaWithId", "http://example.com/schemas/mainExtra#/defs/thing", null, false, "original schema node (id does not match)"), //16. missing node after hash → fallback (MissingNode)
        arguments("schemaWithoutId", "#/missing/node", null, true, "node returned by handleNullNode (missing)"));
    }

    /**
     * Verify that a valid JSON‑Pointer (starting with {@code "#/"} ) is resolved
     * correctly to the target node.
     */
    @Test
    void testGetRefSchemaNode_ValidPointer() {
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
        assertTrue(result.isMissingNode(), "An unknown pointer should yield a missing node after fallback");
    }

    /**
     * Verify that URL‑encoded pointers are decoded before navigation.
     */
    @Test
    void testGetRefSchemaNode_UrlEncodedPointer() {
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
        JsonSchema target = "schemaWithId".equals(schemaKey) ? schemaWithId : schemaWithoutId;
        JsonNode result = target.getRefSchemaNode(ref);
        if (expectMissing) {
            assertTrue(result.isMissingNode(), "Expected a MissingNode for ref \"" + ref + "\"");
            return;
        }
        if (expectedPath == null) {
            // Expect the original root node.
            assertSame(target.getSchemaNode(), result, "Expected the original schema node for ref \"" + ref + "\"");
        } else {
            JsonNode expected = target.getSchemaNode().at(expectedPath);
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:244: error: cannot find symbol
        JsonSchema spy = spy(schemaWithoutId);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:246: error: cannot find symbol
        doReturn(null).when(spy).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:252: error: cannot find symbol
        JsonSchema spy = spy(schemaWithoutId);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:254: error: cannot find symbol
        doReturn(null).when(spy).getSchemaNode();
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchemaNode_Test
6 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method getRefSchemaNode(String)': '\n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\nclass `com.networknt.schema.resource.PrefixSchemaMapper`:\n\trelated functions:\n\t\tmethod `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.resource.MetaSchemaMapper`:\n\trelated functions:\n\t\tmethod `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.DomainValidator`:\n\trelated functions:\n\t\tmethod `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.SchemaLocation.Fragment`:\n\trelated functions:\n\t\tmethod `public static JsonNodePath of(String)` returns `com.networknt.schema.JsonNodePath`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \r\n         * Parses a string representing a fragment.\r\n         * \r\n         * @param fragmentString the fragment\r\n         * @return the path\r\n         ;\nclass `com.networknt.schema.output.OutputUnitData`:\n\trelated functions:\n\t\tmethod `public static String formatMessage(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Validates an IPv4 address. Returns true if valid.\n     * @param inet4Address the IPv4 address to validate\n     * @return true if the argument contains a valid IPv4 address\n     ;\nclass `com.networknt.schema.resource.ClasspathSchemaLoader`:\n\trelated functions:\n\t\tmethod `public InputStreamSource getSchema(AbsoluteIri)` returns `com.networknt.schema.resource.InputStreamSource`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.SchemaLocation`:\n\trelated functions:\n\t\tmethod `public static SchemaLocation of(String)` returns `com.networknt.schema.SchemaLocation`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \r\n     * Parses a string representing an IRI of the schema location.\r\n     * \r\n     * @param iri the IRI\r\n     * @return the schema location\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


