
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
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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

    /**
     * Very small stub of {@link ValidationContext}. Only the methods used by
     * {@link JsonSchema} in the tests are implemented.
     */
    private static class SimpleValidationContext extends ValidationContext {

        private final JsonMetaSchema metaSchema;

        SimpleValidationContext(JsonMetaSchema metaSchema) {
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return null;
        }

        @Override
        public String getCurrentUri() {
            return null;
        }

        @Override
        public URI getCurrentUriObject() {
            return null;
        }

        @Override
        public void setCurrentUri(String uri) {
        }

        @Override
        public void setCurrentUri(URI uri) {
        }

        @Override
        public void setCurrentBaseUri(String uri) {
        }

        @Override
        public void setCurrentBaseUri(URI uri) {
        }
    }

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

        /**
         * Register a node that should be returned for a given fragment.
         */
        void addOverride(String fragment, JsonNode node) {
            overrides.put(fragment, node);
        }

        @Override
        public JsonNode getNodeByFragmentRef(String fragment, JsonNode rootNode) {
            if (overrides.containsKey(fragment)) {
                return overrides.get(fragment);
            }
            JsonNode candidate = rootNode.at(fragment);
            return candidate.isMissingNode() ? null : candidate;
        }

        // The remaining methods of JsonMetaSchema are not needed for these tests.
        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getUri() {
            return null;
        }

        @Override
        public boolean isFormat(String format) {
            return false;
        }

        @Override
        public boolean isKeyword(String keyword) {
            return false;
        }

        @Override
        public boolean isKeywordSupported(String keyword) {
            return false;
        }

        @Override
        public boolean isKeywordSupported(String keyword, boolean strict) {
            return false;
        }

        @Override
        public boolean isFormatSupported(String format) {
            return false;
        }

        @Override
        public boolean isFormatSupported(String format, boolean strict) {
            return false;
        }

        @Override
        public boolean isCustomFormat(String format) {
            return false;
        }

        @Override
        public boolean isCustomKeyword(String keyword) {
            return false;
        }

        @Override
        public boolean isCustomKeyword(String keyword, boolean strict) {
            return false;
        }

        @Override
        public String getFormat(String format) {
            return null;
        }

        @Override
        public String getKeyword(String keyword) {
            return null;
        }
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
        SimpleJsonMetaSchema metaSchema = new SimpleJsonMetaSchema();
        // Build a minimal ValidationContext that returns our stub meta‑schema
        ValidationContext validationContext = new SimpleValidationContext(metaSchema);
        // Build the JsonSchema instance (package‑private factory method)
        jsonSchema = JsonSchema.from(validationContext, "test-schema.json", new URI("http://example.com/test-schema.json"), rootSchemaNode, null, false);
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
        ((SimpleJsonMetaSchema) jsonSchema.getValidationContext().getMetaSchema()).addOverride("#/definitions/address", addressNode);
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
        ((SimpleJsonMetaSchema) jsonSchema.getValidationContext().getMetaSchema()).addOverride("#foo", dummy);
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
        assertSame(rootSchemaNode, result, "Empty reference should return the original node");
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:19: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:23: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:44: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        SimpleValidationContext(JsonMetaSchema metaSchema) {
                                                           ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:58: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:63: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:68: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:72: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:76: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:80: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:96: error: constructor JsonMetaSchema in class JsonMetaSchema cannot be applied to given types;
            super("simple", URI.create("http://example.com/simple"), false, false);
            ^
  required: String,String,Map<String,Keyword>
  found:    String,URI,boolean,boolean
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:116: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:126: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:131: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:136: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:141: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:146: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:151: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:156: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:161: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:166: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:171: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:176: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:338: error: cannot find symbol
        JsonSchema spy = spy(jsonSchema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:339: error: cannot find symbol
        doReturn(null).when(spy).findAncestor();
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:347: error: cannot find symbol
        JsonSchema ancestorMock = mock(JsonSchema.class);
                                  ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:348: error: cannot find symbol
        when(ancestorMock.getSchemaNode()).thenReturn(null);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:349: error: cannot find symbol
        JsonSchema spy = spy(jsonSchema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:350: error: cannot find symbol
        doReturn(ancestorMock).when(spy).findAncestor();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getRefSchemaNode_Test
31 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method getRefSchemaNode(String)': '\n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonMetaSchema com.networknt.schema.JsonSchema.metaSchema;\n\trelated functions:\n\t\tmethod `private static boolean isUriFragmentWithNoContext(URI, String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.uri.URITranslator.PrefixReplacer`:\n\trelated functions:\n\t\tmethod `public URI translate(URI)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.DomainValidator`:\n\trelated functions:\n\t\tmethod `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Validates an IPv4 address. Returns true if valid.\n     *\n     * @param inet4Address the IPv4 address to validate\n     * @return true if the argument contains a valid IPv4 address\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchemaRef getRefSchema(JsonSchema, ValidationContext, String)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.PathType`:\n\trelated functions:\n\t\tmethod ` static String fromJsonPath(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


