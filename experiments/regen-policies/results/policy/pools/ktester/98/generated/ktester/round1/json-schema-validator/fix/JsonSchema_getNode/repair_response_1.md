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

/**
 * Tests for {@link JsonSchema#getNode(Object)} covering various argument types
 * and edge‑cases.
 */
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
        return Stream.of(
                // existing field
                Arguments.of("title", "Example Schema"),
                // missing field
                Arguments.of("missing", null));
    }

    private static Stream<Arguments> provideArrayIndexCases() {
        return Stream.of(
                Arguments.of(0, "first"),
                Arguments.of(1, "second"),
                // out‑of‑range
                Arguments.of(2, null),
                // String supplied – not an index
                Arguments.of("0", null),
                Arguments.of(-1, IllegalArgumentException.class),
                Arguments.of(123L, IllegalArgumentException.class),
                Arguments.of(1.5, IllegalArgumentException.class));
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
        assertEquals("string", nameNode.get("type").asText(),
                "The type of the 'name' property should be 'string'");
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
        assertEquals("string", firstElement.get("type").asText(),
                "The first element's type should be 'string'");
        JsonNode secondElement = schema.getNode(1);
        assertNotNull(secondElement, "Second element of the array schema should not be null");
        assertEquals("integer", secondElement.get("type").asText(),
                "The second element's type should be 'integer'");
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
            assertEquals(expected, result.asText(),
                    "Property '" + property + "' should have expected value");
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
            assertThrows((Class<? extends Throwable>) expected,
                    () -> schema.getNode(index),
                    "Calling getNode(" + index + ") should throw " + ((Class<?>) expected).getSimpleName());
        } else {
            JsonNode result = schema.getNode(index);
            if (expected == null) {
                assertNull(result, "Index " + index + " should yield null");
            } else {
                assertNotNull(result, "Index " + index + " should not be null");
                assertEquals(expected, result.asText(),
                        "Index " + index + " should return expected value");
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
        assertThrows(IllegalArgumentException.class,
                () -> schema.getNode('c'),
                "Character argument should cause IllegalArgumentException");
    }

    /**
     * Arbitrary Object argument – unsupported, must throw {@link IllegalArgumentException}.
     */
    @Test
    void getNode_objectArgument_throwsIllegalArgumentException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IllegalArgumentException.class,
                () -> schema.getNode(new Object()),
                "Object argument should cause IllegalArgumentException");
    }

    /**
     * Null argument – the method does not accept {@code null} and must throw {@link NullPointerException}.
     */
    @Test
    void getNode_nullArgument_throwsNullPointerException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.getNode(null),
                "Passing null should raise NullPointerException");
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
        assertEquals("space key", result.get("description").asText(),
                "Decoded node should contain the expected description");
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
        assertThrows(IllegalArgumentException.class,
                () -> schema.getNode("%ZZ"),
                "Malformed percent‑encoding should cause IllegalArgumentException");
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
        assertThrows(NullPointerException.class,
                () -> schema.getNode("any"),
                "Calling getNode when internal schemaNode is null should raise NullPointerException");
    }
}
```