
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
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /**
     * Factory used for creating real {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * ObjectMapper for turning JSON strings into {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple JSON‑Schema used by the tests.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "http://example.com/simple.json",
          "type": "object",
          "properties": {
            "name": { "type": "string" },
            "age":  { "type": "integer" }
          }
        }
        """;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 meta‑schema – any version works as long as the factory can
        // resolve the schema string above.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Helper that creates a {@link JsonSchema} from the constant {@link #SIMPLE_SCHEMA}.
     */
    private JsonSchema createRootSchema() {
        return factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Helper that creates a {@link JsonSchema} from the supplied JSON schema string.
     */
    private JsonSchema createSchema(String json) {
        return factory.getSchema(json);
    }

    @Test
    void getSubSchema_existingProperty_returnsSubSchema() throws Exception {
        // Arrange – root schema and a fragment that points to an existing subschema.
        JsonSchema root = createRootSchema();
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        // Act – retrieve the subschema.
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert – the subschema is not null and its node describes a string type.
        assertNotNull(sub, "Sub‑schema should be created for an existing fragment");
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(), "The subschema for '/properties/name' must have type 'string'");
    }

    @Test
    void getSubSchema_nonExistingProperty_throwsInvalidSchemaRefException() {
        // Arrange – root schema and a fragment that does **not** exist.
        JsonSchema root = createRootSchema();
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/unknown");
        // Act & Assert – the method must throw the expected exception.
        assertThrows(InvalidSchemaRefException.class, () -> root.getSubSchema(fragment), "Requesting a non‑existent fragment should result in InvalidSchemaRefException");
    }

    @Test
    void getSubSchema_arrayIndexSegment_returnsSubSchema() {
        // Arrange – a schema with an "items" array.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "array",
              "items": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/items/0");
        // Act
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert
        assertNotNull(sub, "Sub‑schema should be created for an existing array element");
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(), "The first element of \"items\" must be a string schema");
        assertEquals("#/items/0", sub.getSchemaLocation().getFragment(), "SchemaLocation fragment must reflect the traversed pointer");
    }

    @Test
    void getSubSchema_objectPropertySegment_returnsSubSchema() {
        // Arrange – simple object schema (already defined in the framework).
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/simple.json",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        // Act
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText());
        assertEquals("#/properties/name", sub.getSchemaLocation().getFragment());
    }

    @Test
    void getSubSchema_idResolutionCreatesIntermediateSubSchema() {
        // Arrange – a definition that introduces a new $id.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/root.json",
              "definitions": {
                "address": {
                  "$id": "http://example.com/address.json",
                  "type": "object",
                  "properties": {
                    "street": { "type": "string" }
                  }
                }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/definitions/address/properties/street");
        // Act
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(), "The 'street' property must be a string");
        assertEquals("#/definitions/address/properties/street", sub.getSchemaLocation().getFragment(), "Final fragment must contain the whole pointer");
        // The intermediate $id creates a new lexical context – we can verify that the
        // sub‑schema's id matches the $id defined in the address definition.
        assertEquals("http://example.com/address.json", sub.getId(), "Lexical $id should be honoured");
    }

    @Test
    void getSubSchema_numericSegmentOnObject_isTreatedAsString() {
        // Arrange – a schema where the property name "0" exists.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "0": { "type": "string" }
              }
            }""";
        JsonSchema root = createSchema(schema);
        // Mock a JsonNodePath that returns a Number for the second segment.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getNameCount()).thenReturn(2);
        when(fragment.getElement(0)).thenReturn("properties");
        when(fragment.getElement(1)).thenReturn(Integer.valueOf(0));
        // The toString / getFragment is not used by getSubSchema, but some
        // validation messages may call it – provide a sensible value.
        when(fragment.toString()).thenReturn("#/properties/0");
        // Act
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert
        assertNotNull(sub, "Sub‑schema should be created for property named \"0\"");
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(), "Property \"0\" must be a string");
        assertEquals("#/properties/0", sub.getSchemaLocation().getFragment(), "Fragment must be reproduced unchanged");
    }

    @Test
    void getSubSchema_fragmentNotFound_andExternalResourceReturnsNull_throwsInvalidSchemaRefException(@Mock JsonSchema documentMock) {
        // Arrange – a simple root schema without the fragment.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }""";
        JsonSchema realRoot = createSchema(schema);
        JsonSchema root = spy(realRoot);
        // make findSchemaResourceRoot() return our mocked document
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        // document's own findSchemaResourceRoot() returns itself (no further nesting)
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // fetchSubSchemaNode returns null → the method should throw InvalidSchemaRefException
        when(documentMock.fetchSubSchemaNode(any())).thenReturn(null);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/nonexistent/path");
        // Act & Assert
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class, () -> root.getSubSchema(fragment), "When the fragment cannot be resolved and no external resource is found, " + "InvalidSchemaRefException must be thrown");
        // Verify that the ValidationMessage contains the expected type/code.
        ValidationMessage vm = ex.getValidationMessage();
        assertEquals(ValidatorTypeCode.REF.getValue(), vm.getType(), "ValidationMessage type must be REF");
        assertEquals("internal.unresolvedRef", vm.getCode(), "ValidationMessage code must be internal.unresolvedRef");
        assertTrue(vm.getMessage().contains("Reference"), "ValidationMessage should contain a human readable description");
    }

    @Test
    void getSubSchema_fragmentNotFound_externalSchemaResolves_success(@Mock JsonSchema documentMock, @Mock JsonSchema externalMock, @Mock JsonSchema expectedSub) {
        // Arrange – root schema without the fragment.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }""";
        JsonSchema realRoot = createSchema(schema);
        JsonSchema root = spy(realRoot);
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // fetchSubSchemaNode returns an external schema that can resolve the fragment.
        when(documentMock.fetchSubSchemaNode(any())).thenReturn(externalMock);
        // The external schema's getSubSchema returns the expected sub‑schema.
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/external/ref");
        when(externalMock.getSubSchema(fragment)).thenReturn(expectedSub);
        // Act
        JsonSchema result = root.getSubSchema(fragment);
        // Assert
        assertSame(expectedSub, result, "The sub‑schema resolved by the external resource must be returned");
    }

    @Test
    void getSubSchema_lastSegmentWithoutId_createsOnlyFinalSubSchema() {
        // Arrange – simple object schema (same as earlier).
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/simple.json",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/age");
        // Act
        JsonSchema sub = root.getSubSchema(fragment);
        // Assert
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("integer", subNode.get("type").asText(), "The 'age' property must be an integer");
        assertEquals("#/properties/age", sub.getSchemaLocation().getFragment(), "Fragment must be exactly the final pointer");
    }

    @Test
    void getSubSchema_nullFragment_throwsNullPointerException() {
        JsonSchema root = createSchema("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> root.getSubSchema(null), "Calling getSubSchema with a null fragment must throw NPE");
    }

    @Test
    void getSubSchema_unsupportedSegment_throwsIllegalArgumentException() {
        // Arrange – a simple schema.
        JsonSchema root = createSchema("{\"type\":\"object\",\"properties\":{\"active\":{\"type\":\"boolean\"}}}");
        // Create a JsonNodePath where the second segment is a Boolean.
        JsonNodePath fragment = new JsonNodePath() {

            @Override
            public int getNameCount() {
                return 2;
            }

            @Override
            public Object getElement(int index) {
                return index == 0 ? "properties" : Boolean.TRUE;
            }

            @Override
            public String toString() {
                return "#/properties/active";
            }

            @Override
            public String getFragment() {
                return "#/properties/active";
            }
        };
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> root.getSubSchema(fragment), "A non‑String/Number segment should cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("segment"), "Exception message should mention the problematic segment");
    }

    @Test
    void getSubSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        // Arrange – create a normal schema and then null‑out its validationContext.
        JsonSchema root = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // Use reflection to set the protected field BaseJsonValidator.validationContext to null.
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(root, null);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        // Act & Assert
        assertThrows(NullPointerException.class, () -> root.getSubSchema(fragment), "If the ValidationContext is null, a NPE should be thrown");
    }

    @Test
    void getSubSchema_fetchSubSchemaNodeThrowsRuntimeException_propagates(@Mock JsonSchema documentMock) {
        // Arrange – root schema without the fragment.
        JsonSchema root = spy(createSchema("{\"type\":\"object\"}"));
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // Simulate a runtime exception from fetchSubSchemaNode.
        when(documentMock.fetchSubSchemaNode(any())).thenThrow(new IllegalStateException("fetch failed"));
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/nonexistent");
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> root.getSubSchema(fragment), "Runtime exceptions from fetchSubSchemaNode must propagate unchanged");
        assertEquals("fetch failed", ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:9: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:12: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:191: error: cannot find symbol
    void getSubSchema_fragmentNotFound_andExternalResourceReturnsNull_throwsInvalidSchemaRefException(@Mock JsonSchema documentMock) {
                                                                                                       ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:217: error: cannot find symbol
    void getSubSchema_fragmentNotFound_externalSchemaResolves_success(@Mock JsonSchema documentMock, @Mock JsonSchema externalMock, @Mock JsonSchema expectedSub) {
                                                                       ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:217: error: cannot find symbol
    void getSubSchema_fragmentNotFound_externalSchemaResolves_success(@Mock JsonSchema documentMock, @Mock JsonSchema externalMock, @Mock JsonSchema expectedSub) {
                                                                                                      ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:217: error: cannot find symbol
    void getSubSchema_fragmentNotFound_externalSchemaResolves_success(@Mock JsonSchema documentMock, @Mock JsonSchema externalMock, @Mock JsonSchema expectedSub) {
                                                                                                                                     ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:314: error: cannot find symbol
    void getSubSchema_fetchSubSchemaNodeThrowsRuntimeException_propagates(@Mock JsonSchema documentMock) {
                                                                           ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:174: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:175: error: cannot find symbol
        when(fragment.getNameCount()).thenReturn(2);
        ^
  symbol:   method when(int)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:176: error: cannot find symbol
        when(fragment.getElement(0)).thenReturn("properties");
        ^
  symbol:   method when(Object)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:177: error: cannot find symbol
        when(fragment.getElement(1)).thenReturn(Integer.valueOf(0));
        ^
  symbol:   method when(Object)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:180: error: cannot find symbol
        when(fragment.toString()).thenReturn("#/properties/0");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:199: error: cannot find symbol
        JsonSchema root = spy(realRoot);
                          ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:201: error: cannot find symbol
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:203: error: cannot find symbol
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:205: error: cannot find symbol
        when(documentMock.fetchSubSchemaNode(any())).thenReturn(null);
                                             ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:225: error: cannot find symbol
        JsonSchema root = spy(realRoot);
                          ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:226: error: cannot find symbol
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:227: error: cannot find symbol
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:229: error: cannot find symbol
        when(documentMock.fetchSubSchemaNode(any())).thenReturn(externalMock);
                                             ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:232: error: cannot find symbol
        when(externalMock.getSubSchema(fragment)).thenReturn(expectedSub);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:273: error: no suitable constructor found for JsonNodePath(no arguments)
        JsonNodePath fragment = new JsonNodePath() {
                                ^
    constructor JsonNodePath.JsonNodePath(PathType) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,String) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,int) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:290: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:316: error: cannot find symbol
        JsonSchema root = spy(createSchema("{\"type\":\"object\"}"));
                          ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:317: error: cannot find symbol
        when(root.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:318: error: cannot find symbol
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:320: error: cannot find symbol
        when(documentMock.fetchSubSchemaNode(any())).thenThrow(new IllegalStateException("fetch failed"));
                                             ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
28 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method getSubSchema(JsonNodePath)': '\n     * Gets the sub schema given the json pointer fragment.\n     * \n     * @param fragment the json pointer fragment\n     * @return the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessage`:\n\t\nclass `com.networknt.schema.ValidatorTypeCode`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidatorTypeCode com.networknt.schema.ValidatorTypeCode.REF;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`, api document: \n     * Please note that the key in {@link #validators} map is the evaluation path.\n     ;\n\t\tmethod `private JsonNode handleNullNode(String, JsonSchema)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


