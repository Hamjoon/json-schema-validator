
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    @Test
    void getRefSchemaNode_nullRoot_throwsNPE() {
        JsonSchema spy = spy(schema);
        // Stub findSchemaResourceRoot() to return null
        doReturn(null).when(spy).findSchemaResourceRoot();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/pointer"), "When the root schema is null, a NullPointerException is expected");
    }

    /**
     * Simulates a valid root schema but its {@code getSchemaNode()} returns {@code null}.
     * When the reference starts with '/', the method will attempt {@code node.at(...)} on a null
     * node, resulting in a {@link NullPointerException}.
     */
    @Test
    void getRefSchemaNode_nullRootNode_throwsNPE() {
        // Create a real schema (so that internal fields are initialised) and then spy it.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema real = factory.getSchema(SIMPLE_SCHEMA);
        JsonSchema spy = spy(real);
        // Stub getSchemaNode() on the root schema returned by findSchemaResourceRoot()
        JsonSchema rootMock = mock(JsonSchema.class);
        when(rootMock.getId()).thenReturn(null);
        when(rootMock.getSchemaNode()).thenReturn(null);
        doReturn(rootMock).when(spy).findSchemaResourceRoot();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/some/path"), "When the root schema node is null, a NullPointerException is expected");
    }

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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:135: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:137: error: cannot find symbol
        doReturn(null).when(spy).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:151: error: cannot find symbol
        JsonSchema spy = spy(real);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:153: error: cannot find symbol
        JsonSchema rootMock = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:154: error: cannot find symbol
        when(rootMock.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:155: error: cannot find symbol
        when(rootMock.getSchemaNode()).thenReturn(null);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:156: error: cannot find symbol
        doReturn(rootMock).when(spy).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
9 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method getRefSchemaNode(String)': '\n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\nclass `com.networknt.schema.resource.PrefixSchemaMapper`:\n\trelated functions:\n\t\tmethod `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.resource.MetaSchemaMapper`:\n\trelated functions:\n\t\tmethod `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.DomainValidator`:\n\trelated functions:\n\t\tmethod `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.output.OutputUnitData`:\n\trelated functions:\n\t\tmethod `public static String formatMessage(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Validates an IPv4 address. Returns true if valid.\n     * @param inet4Address the IPv4 address to validate\n     * @return true if the argument contains a valid IPv4 address\n     ;\nclass `com.networknt.schema.resource.ClasspathSchemaLoader`:\n\trelated functions:\n\t\tmethod `public InputStreamSource getSchema(AbsoluteIri)` returns `com.networknt.schema.resource.InputStreamSource`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.SchemaLocation`:\n\trelated functions:\n\t\tmethod `public static SchemaLocation of(String)` returns `com.networknt.schema.SchemaLocation`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \r\n     * Parses a string representing an IRI of the schema location.\r\n     * \r\n     * @param iri the IRI\r\n     * @return the schema location\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


