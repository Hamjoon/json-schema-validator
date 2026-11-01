
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
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    /**
     * Factory used for creating {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent stable version shipped with the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release, but the method is kept for symmetry with the template.
        factory = null;
    }

    private static Stream<Arguments> providePropertyNameCases() {
        return Stream.of(// existing field
        Arguments.of("title", "Example Schema"), // missing field
        Arguments.of("missing", null));
    }

    private static Stream<Arguments> provideArrayIndexCases() {
        return Stream.of(Arguments.of(0, "first"), Arguments.of(1, "second"), // out‑of‑range
        Arguments.of(2, null), // String supplied – not an index
        Arguments.of("0", null), Arguments.of(-1, IllegalArgumentException.class), Arguments.of(123L, IllegalArgumentException.class), Arguments.of(1.5, IllegalArgumentException.class));
    }

    /**
     * Verify that {@link JsonSchema#getNode(Object)} correctly retrieves a child
     * property from the underlying schema node when the argument is a {@link String}.
     */
    @Test
    void getNode_withPropertyName_returnsCorrespondingNode() {
        // A minimal object schema with a single property "name".
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // The protected method is visible because the test lives in the same package.
        JsonNode propertiesNode = schema.getNode("properties");
        assertNotNull(propertiesNode, "properties node should not be null");
        assertTrue(propertiesNode.isObject(), "properties node should be an object");
        JsonNode nameNode = propertiesNode.get("name");
        assertNotNull(nameNode, "name node should exist inside properties");
        assertEquals("string", nameNode.get("type").asText(), "The type of the 'name' property should be 'string'");
    }

    /**
     * Verify that {@link JsonSchema#getNode(Object)} correctly retrieves an element
     * from an array schema when the argument is an {@link Integer}.
     */
    @Test
    void getNode_withArrayIndex_returnsCorrespondingElement() {
        // An array schema – each element is itself a schema object.
        String schemaJson = """
            [
              { "type": "string" },
              { "type": "integer" }
            ]
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode firstElement = schema.getNode(0);
        assertNotNull(firstElement, "First element of the array schema should not be null");
        assertTrue(firstElement.isObject(), "First element should be an object node");
        assertEquals("string", firstElement.get("type").asText(), "The first element's type should be 'string'");
        JsonNode secondElement = schema.getNode(1);
        assertNotNull(secondElement, "Second element of the array schema should not be null");
        assertEquals("integer", secondElement.get("type").asText(), "The second element's type should be 'integer'");
    }

    /**
     * Verify that a non‑existent property returns {@code null} rather than throwing.
     */
    @Test
    void getNode_nonExistingProperty_returnsNull() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode missing = schema.getNode("unknown");
        assertNull(missing, "Requesting a non‑existing property should return null");
    }

    /**
     * Verify that an out‑of‑range array index returns {@code null}.
     */
    @Test
    void getNode_arrayIndexOutOfRange_returnsNull() {
        String schemaJson = """
            [
              { "type": "boolean" }
            ]
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode outOfRange = schema.getNode(5);
        assertNull(outOfRange, "Array index beyond bounds should return null");
    }

    /**
     * Property‑name access – both existing and missing fields.
     */
    @ParameterizedTest(name = "[{index}] property={0} → expected={1}")
    @MethodSource("providePropertyNameCases")
    void getNode_propertyName_cases(String property, Object expected) {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Example Schema",
              "type": "object",
              "properties": {
                "title": { "type": "string" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode(property);
        if (expected == null) {
            assertNull(result, "Property '" + property + "' should yield null");
        } else {
            assertNotNull(result, "Property '" + property + "' should not be null");
            assertEquals(expected, result.asText(), "Property '" + property + "' should have expected value");
        }
    }

    /**
     * Array index access – valid indexes, out‑of‑range and non‑integer numeric types.
     */
    @ParameterizedTest(name = "[{index}] index={0} → expected={1}")
    @MethodSource("provideArrayIndexCases")
    void getNode_arrayIndex_cases(Object index, Object expected) {
        String schemaJson = """
            ["first","second"]
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        if (expected instanceof Class) {
            // Expected exception
            assertThrows((Class<? extends Throwable>) expected, () -> schema.getNode(index), "Calling getNode(" + index + ") should throw " + ((Class<?>) expected).getSimpleName());
        } else {
            JsonNode result = schema.getNode(index);
            if (expected == null) {
                assertNull(result, "Index " + index + " should yield null");
            } else {
                assertNotNull(result, "Index " + index + " should not be null");
                assertEquals(expected, result.asText(), "Index " + index + " should return expected value");
            }
        }
    }

    /**
     * Boolean argument – should be ignored and return {@code null}.
     */
    @Test
    void getNode_booleanArgument_returnsNull() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode(true);
        assertNull(result, "Boolean argument should result in null");
    }

    /**
     * Character argument – unsupported, must throw {@link IllegalArgumentException}.
     */
    @Test
    void getNode_characterArgument_throwsIllegalArgumentException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> schema.getNode('c'), "Character argument should cause IllegalArgumentException");
    }

    /**
     * Arbitrary Object argument – unsupported, must throw {@link IllegalArgumentException}.
     */
    @Test
    void getNode_objectArgument_throwsIllegalArgumentException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> schema.getNode(new Object()), "Object argument should cause IllegalArgumentException");
    }

    /**
     * Null argument – the method does not accept {@code null} and must throw {@link NullPointerException}.
     */
    @Test
    void getNode_nullArgument_throwsNullPointerException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class, () -> schema.getNode(null), "Passing null should raise NullPointerException");
    }

    /**
     * When the underlying {@code schemaNode} is a primitive/value node,
     * any call to {@code getNode} must return {@code null}.
     */
    @Test
    void getNode_onValueNode_alwaysReturnsNull() {
        // The schema itself is a JSON string node (a value node)
        String schemaJson = "\"just a text\"";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode("any");
        assertNull(result, "Any property lookup on a value node should return null");
    }

    /**
     * URL‑decoded property name – the method should decode the supplied string
     * before looking up the field.
     */
    @Test
    void getNode_urlEncodedPropertyName_returnsDecodedNode() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "a b": { "description": "space key" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode("a%20b");
        assertNotNull(result, "Decoded property name should be found");
        assertEquals("space key", result.get("description").asText(), "Decoded node should contain the expected description");
    }

    /**
     * Invalid URL‑encoded property name – decoding fails and the method should
     * propagate an {@link IllegalArgumentException}.
     */
    @Test
    void getNode_invalidUrlEncodedPropertyName_throwsIllegalArgumentException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "bad": { "type": "string" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> schema.getNode("%ZZ"), "Malformed percent‑encoding should cause IllegalArgumentException");
    }

    /**
     * Schema node is {@code null}.  We create a normal {@link JsonSchema}
     * instance and then, via reflection, set its internal {@code schemaNode}
     * field to {@code null}.  Any subsequent call to {@code getNode} must
     * throw {@link NullPointerException}.
     */
    @Test
    void getNode_schemaNodeIsNull_throwsNullPointerException() throws Exception {
        // Normal schema – we will null‑out its internal node later.
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        // Use reflection to set the protected field defined in BaseJsonValidator.
        Field nodeField = BaseJsonValidator.class.getDeclaredField("schemaNode");
        nodeField.setAccessible(true);
        nodeField.set(schema, null);
        assertThrows(NullPointerException.class, () -> schema.getNode("any"), "Calling getNode when internal schemaNode is null should raise NullPointerException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getNode_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getNode_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
Note: src/test/java/com/networknt/schema/JsonSchema_getNode_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validateUnknownFormat(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: \r\n     * When the Format-Assertion vocabulary is specified, implementations MUST fail upon encountering unknown formats.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @param node the node\r\n     * @param rootNode the root node\r\n     * @param instanceLocation the instance location\r\n     * @return the messages\r\n     ;\n\t\tmethod `protected Object getAnnotationValue()` returns `java.lang.Object`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: \r\n     * Gets the annotation value.\r\n     * \r\n     * @return the annotation value\r\n     ;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


