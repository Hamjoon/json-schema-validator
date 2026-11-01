


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Simple schema used for all tests
     */
    private static final String SIMPLE_SCHEMA = "{\n" + "  \"$id\": \"http://example.com/simple-schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" },\n" + "    \"age\":  { \"type\": \"integer\" }\n" + "  }\n" + "}";

    private JsonSchema schema;

    /**
     * A richer schema that contains an $id, definitions and properties with spaces/encoded chars.
     */
    private static final String COMPLEX_SCHEMA = "{\n" + "  \"$id\": \"http://example.com/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" },\n" + "    \"age\": { \"type\": \"integer\" },\n" + "    \"properties name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"definitions\": {\n" + "    \"person\": { \"type\": \"object\", \"properties\": { \"first\": {\"type\":\"string\"} } },\n" + "    \"address\": { \"type\": \"object\", \"properties\": { \"city\": {\"type\":\"string\"} } }\n" + "  }\n" + "}";

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(this.schema, "Schema should be created successfully");
    }

    /**
     * Provides arguments for the parameterised test {@link #parameterisedRefResolution}.
     *
     * <p>Each argument consists of:
     *
     * <ul>
     *   <li>the reference string supplied to {@code getRefSchemaNode}</li>
     *   <li>a description used only for readability in the test name</li>
     *   <li>a {@code java.util.function.Predicate<JsonNode>} that validates the returned node</li>
     * </ul>
     */
    private static Stream<Arguments> refResolutionProvider() {
        return Stream.of(// Simple JSON‑Pointer
        Arguments.of("/properties/name", "no ID, leading '/' – direct pointer", (java.util.function.Predicate<JsonNode>) node -> node != null && node.equals(MAPPER.createObjectNode().put("type", "string"))), // Full URI with pointer
        Arguments.of("http://example.com/schema/properties/age", "ID stripped, pointer resolved", (java.util.function.Predicate<JsonNode>) node -> node != null && node.equals(MAPPER.createObjectNode().put("type", "integer"))), // Fragment handling
        Arguments.of("#/definitions/person", "fragment '#/' stripped", (java.util.function.Predicate<JsonNode>) node -> node != null && node.path("type").asText().equals("object")), Arguments.of("http://example.com/schema#/definitions/address", "ID + fragment '#/' stripped", (java.util.function.Predicate<JsonNode>) node -> node != null && node.path("type").asText().equals("object")), // No leading '/' after processing – should return root node
        Arguments.of("definitions/simple", "no leading '/' after processing – root node", (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()), // URL decoding of encoded characters (%2F)
        Arguments.of("/properties%2Fname", "encoded '/' decoded", (java.util.function.Predicate<JsonNode>) node -> node != null && node.equals(MAPPER.createObjectNode().put("type", "string"))), // Null reference – expect NPE
        Arguments.of(null, "null reference – NPE", null), // Empty reference – should return root node unchanged
        Arguments.of("", "empty reference – root node", (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()), // Exact ID match – empty pointer → root node
        Arguments.of("http://example.com/schema", "exact ID match – root node", (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()), // Reference without leading slash after ID stripping
        Arguments.of("http://example.com/schemaproperties/name", "ID stripped, no leading '/' – root node", (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()), // '+' character decoding to space
        Arguments.of("/properties+name", "plus decoded to space", (java.util.function.Predicate<JsonNode>) node -> node != null && node.equals(MAPPER.createObjectNode().put("type", "string"))), // Encoded space (%20)
        Arguments.of("/properties%20name", "encoded space decoded", (java.util.function.Predicate<JsonNode>) node -> node != null && node.equals(MAPPER.createObjectNode().put("type", "string"))), // Fragment only reference without leading slash – root node
        Arguments.of("#definitions/person", "fragment without '#/' – root node", (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()), // Decoded missing path – expect missing node
        Arguments.of("http://example.com/schema#/nonexistent%2Fpath", "decoded missing path – missing node", null));
    }

    /**
     * Verify that a reference that starts with the schema's $id is correctly
     * resolved to the same node as a plain JSON‑Pointer reference.
     */
    @Test
    void getRefSchemaNode_WithIdPrefix_resolvesCorrectly() throws Exception {
        String refWithId = "http://example.com/simple-schema.json#/properties/name";
        JsonNode result = schema.getRefSchemaNode(refWithId);
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
        JsonNode expected = MAPPER.readTree("{\"type\":\"string\"}");
        assertEquals(expected, result, "Resolved node should match the \"name\" property schema");
    }

    /**
     * Verify that a plain JSON‑Pointer reference (starting with \"#/\") is
     * resolved to the correct sub‑schema node.
     */
    @Test
    void getRefSchemaNode_WithPlainPointer_resolvesCorrectly() throws Exception {
        String plainRef = "#/properties/age";
        JsonNode result = schema.getRefSchemaNode(plainRef);
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
        JsonNode expected = MAPPER.readTree("{\"type\":\"integer\"}");
        assertEquals(expected, result, "Resolved node should match the \"age\" property schema");
    }

    /**
     * Verify that a reference that points to a non‑existent location falls back
     * to {@code handleNullNode}. In the current implementation {@code handleNullNode}
     * returns {@code null} when it cannot resolve the reference, so the method
     * should return {@code null} (or a missing node) for such a case.
     */
    @Test
    void getRefSchemaNode_NonExistingPointer_returnsMissingOrNull() {
        String missingRef = "#/properties/unknown";
        JsonNode result = schema.getRefSchemaNode(missingRef);
        assertTrue(result == null || result.isMissingNode(), "Result should be null or a missing node for a non‑existent reference");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("refResolutionProvider")
    void parameterisedRefResolution(String ref, String description, java.util.function.Predicate<JsonNode> validator) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(COMPLEX_SCHEMA);
        assertNotNull(complexSchema, "Complex schema must be created");
        if (ref == null) {
            // Expect a NullPointerException because the implementation does not guard against null.
            assertThrows(NullPointerException.class, () -> complexSchema.getRefSchemaNode(ref), "Calling getRefSchemaNode(null) should throw NPE");
            return;
        }
        JsonNode result = complexSchema.getRefSchemaNode(ref);
        if (validator != null) {
            assertNotNull(result, "Resulting node must not be null");
            assertTrue(validator.test(result), () -> "Validator failed for reference [" + ref + "], result: " + result);
        } else {
            // Expected missing node or null when the pointer cannot be resolved.
            assertTrue(result == null || result.isMissingNode(), () -> "Expected missing node or null for reference [" + ref + "], but got: " + result);
        }
    }

    /**
     * Simulates {@code findSchemaResourceRoot()} returning {@code null}.
     * The subsequent call to {@code schema.getId()} should trigger a
     * {@link NullPointerException}.
     */
//     @Test
//     void getRefSchemaNode_nullRoot_throwsNPE() {
//         JsonSchema spy = spy(schema);
//         // Stub findSchemaResourceRoot() to return null
//         doReturn(null).when(spy).findSchemaResourceRoot();
//         assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/pointer"), "When the root schema is null, a NullPointerException is expected");
//     }

    /**
     * Simulates a valid root schema but its {@code getSchemaNode()} returns {@code null}.
     * When the reference starts with '/', the method will attempt {@code node.at(...)} on a null
     * node, resulting in a {@link NullPointerException}.
     */
//     @Test
//     void getRefSchemaNode_nullRootNode_throwsNPE() {
//         // Create a real schema (so that internal fields are initialised) and then spy it.
//         JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
//         JsonSchema real = factory.getSchema(SIMPLE_SCHEMA);
//         JsonSchema spy = spy(real);
//         // Stub getSchemaNode() on the root schema returned by findSchemaResourceRoot()
//         JsonSchema rootMock = mock(JsonSchema.class);
//         when(rootMock.getId()).thenReturn(null);
//         when(rootMock.getSchemaNode()).thenReturn(null);
//         doReturn(rootMock).when(spy).findSchemaResourceRoot();
//         assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/some/path"), "When the root schema node is null, a NullPointerException is expected");
//     }

    /**
     * Verifies that when the JSON‑Pointer resolves to a missing node, the private
     * {@code handleNullNode} method is invoked. The current implementation of
     * {@code handleNullNode} returns {@code null}, therefore the public method
     * should return {@code null} (or a MissingNode) for such a reference.
     */
    @Test
    void getRefSchemaNode_missingNode_triggersHandleNullNode() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(COMPLEX_SCHEMA);
        assertNotNull(complexSchema);
        String ref = "http://example.com/schema#/nonexistent%2Fpath";
        JsonNode result = complexSchema.getRefSchemaNode(ref);
        assertTrue(result == null || result.isMissingNode(), "Result should be null or MissingNode when the pointer cannot be resolved");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_getRefSchemaNode_Test ✔
│     ├─ getRefSchemaNode_WithIdPrefix_resolvesCorrectly() ✔
│     ├─ getRefSchemaNode_WithPlainPointer_resolvesCorrectly() ✔
│     ├─ parameterisedRefResolution(String, String, Predicate) ✔
│     │  ├─ [1] no ID, leading '/' – direct pointer ✔
│     │  ├─ [2] ID stripped, pointer resolved ✔
│     │  ├─ [3] fragment '#/' stripped ✔
│     │  ├─ [4] ID + fragment '#/' stripped ✔
│     │  ├─ [5] no leading '/' after processing – root node ✔
│     │  ├─ [6] encoded '/' decoded ✔
│     │  ├─ [7] null reference – NPE ✔
│     │  ├─ [8] empty reference – root node ✔
│     │  ├─ [9] exact ID match – root node ✔
│     │  ├─ [10] ID stripped, no leading '/' – root node ✔
│     │  ├─ [11] plus decoded to space ✘ Resulting node must not be null ==> expected: not <null>
│     │  ├─ [12] encoded space decoded ✘ Resulting node must not be null ==> expected: not <null>
│     │  ├─ [13] fragment without '#/' – root node ✔
│     │  └─ [14] decoded missing path – missing node ✔
│     ├─ getRefSchemaNode_missingNode_triggersHandleNullNode() ✔
│     └─ getRefSchemaNode_NonExistingPointer_returnsMissingOrNull() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:parameterisedRefResolution(String, String, Predicate):[11] plus decoded to space
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'parameterisedRefResolution', methodParameterTypes = 'java.lang.String, java.lang.String, java.util.function.Predicate']
    => org.opentest4j.AssertionFailedError: Resulting node must not be null ==> expected: not <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotNull.failNull(AssertNotNull.java:49)
       org.junit.jupiter.api.AssertNotNull.assertNotNull(AssertNotNull.java:35)
       org.junit.jupiter.api.Assertions.assertNotNull(Assertions.java:312)
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.parameterisedRefResolution(JsonSchema_getRefSchemaNode_Test.java:118)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:parameterisedRefResolution(String, String, Predicate):[12] encoded space decoded
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'parameterisedRefResolution', methodParameterTypes = 'java.lang.String, java.lang.String, java.util.function.Predicate']
    => org.opentest4j.AssertionFailedError: Resulting node must not be null ==> expected: not <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotNull.failNull(AssertNotNull.java:49)
       org.junit.jupiter.api.AssertNotNull.assertNotNull(AssertNotNull.java:35)
       org.junit.jupiter.api.Assertions.assertNotNull(Assertions.java:312)
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.parameterisedRefResolution(JsonSchema_getRefSchemaNode_Test.java:118)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 364 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        18 tests found           ]
[         0 tests skipped         ]
[        18 tests started         ]
[         0 tests aborted         ]
[        16 tests successful      ]
[         2 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method getRefSchemaNode(String): 

     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     

return type: 
com.fasterxml.jackson.databind.JsonNode

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     ;
class `com.networknt.schema.resource.PrefixSchemaMapper`:
	related functions:
		method `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.resource.MetaSchemaMapper`:
	related functions:
		method `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.org.apache.commons.validator.routines.DomainValidator`:
	related functions:
		method `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.output.OutputUnitData`:
	related functions:
		method `public static String formatMessage(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Validates an IPv4 address. Returns true if valid.
     * @param inet4Address the IPv4 address to validate
     * @return true if the argument contains a valid IPv4 address
     ;
class `com.networknt.schema.resource.ClasspathSchemaLoader`:
	related functions:
		method `public InputStreamSource getSchema(AbsoluteIri)` returns `com.networknt.schema.resource.InputStreamSource`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.SchemaLocation`:
	related functions:
		method `public static SchemaLocation of(String)` returns `com.networknt.schema.SchemaLocation`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Parses a string representing an IRI of the schema location.
     * 
     * @param iri the IRI
     * @return the schema location
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
