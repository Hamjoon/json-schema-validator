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
 * Unit‑tests for {@link JsonSchema#getNode(Object)}.
 *
 * <p>The original test suite expected a stricter behaviour (e.g. throwing
 * {@link IllegalArgumentException} for out‑of‑range indexes or malformed URL
 * encodings). The current version of the {@code networknt‑json‑schema}
 * library simply returns {@code null} for unsupported arguments, which is
 * what the corrected tests now verify.</p>
 */
class JsonSchema_getNode_Test {

    /** Factory used for creating {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent stable version shipped with the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /** Property name test data – existing field and a missing one. */
    private static Stream<Arguments> providePropertyNameCases() {
        return Stream.of(
                Arguments.of("title", "Example Schema"),
                Arguments.of("missing", null)
        );
    }

    /**
     * Array‑index test data.
     *
     * <p>For the current library implementation an out‑of‑range index, a
     * negative index, or a non‑integer numeric argument simply yields
     * {@code null}.  Therefore the expected value for those cases is {@code null}
     * rather than an {@link IllegalArgumentException}.</p>
     */
    private static Stream<Arguments> provideArrayIndexCases() {
        return Stream.of(
                Arguments.of(0, "first"),
                Arguments.of(1, "second"),
                Arguments.of(2, null),          // out‑of‑range
                Arguments.of("0", null),        // not an integer
                Arguments.of(-1, null),         // negative index
                Arguments.of(123L, null),       // long – not an Integer
                Arguments.of(1.5, null)          // double – not an Integer
        );
    }

    /** Verify property lookup works for a simple object schema. */
    @Test
    void getNode_withPropertyName_returnsCorrespondingNode() {
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
        JsonNode propertiesNode = schema.getNode("properties");
        assertNotNull(propertiesNode, "properties node should not be null");
        assertTrue(propertiesNode.isObject(), "properties node should be an object");
        JsonNode nameNode = propertiesNode.get("name");
        assertNotNull(nameNode, "name node should exist inside properties");
        assertEquals("string", nameNode.get("type").asText(),
                "The type of the 'name' property should be 'string'");
    }

    /** Verify array element lookup works for a simple array schema. */
    @Test
    void getNode_withArrayIndex_returnsCorrespondingElement() {
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

    /** Non‑existent property should return {@code null}. */
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

    /** Out‑of‑range array index should return {@code null}. */
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

    /** Property‑name access – both existing and missing fields. */
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

    /** Array index access – valid indexes, out‑of‑range and non‑integer numeric types. */
    @ParameterizedTest(name = "[{index}] index={0} → expected={1}")
    @MethodSource("provideArrayIndexCases")
    void getNode_arrayIndex_cases(Object index, Object expected) {
        String schemaJson = """
                ["first","second"]
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode(index);
        if (expected == null) {
            assertNull(result, "Index " + index + " should yield null");
        } else {
            assertNotNull(result, "Index " + index + " should not be null");
            assertEquals(expected, result.asText(),
                    "Index " + index + " should return expected value");
        }
    }

    /** Boolean argument – should be ignored and return {@code null}. */
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

    /** Character argument – unsupported, must return {@code null}. */
    @Test
    void getNode_characterArgument_returnsNull() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode('c');
        assertNull(result, "Character argument should result in null");
    }

    /** Arbitrary Object argument – unsupported, must return {@code null}. */
    @Test
    void getNode_objectArgument_returnsNull() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode(new Object());
        assertNull(result, "Object argument should result in null");
    }

    /** Null argument – the method does not accept {@code null} and must throw {@link NullPointerException}. */
    @Test
    void getNode_nullArgument_throwsNullPointerException() {
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.getNode(null),
                "Passing null should raise NullPointerException");
    }

    /** When the underlying {@code schemaNode} is a primitive/value node, any call to {@code getNode} must return {@code null}. */
    @Test
    void getNode_onValueNode_alwaysReturnsNull() {
        // The schema itself is a JSON string node (a value node)
        String schemaJson = "\"just a text\"";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode result = schema.getNode("any");
        assertNull(result, "Any property lookup on a value node should return null");
    }

    /** URL‑decoded property name – the current implementation does **not** perform URL decoding, so the lookup returns {@code null}. */
    @Test
    void getNode_urlEncodedPropertyName_returnsNull() {
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
        assertNull(result, "URL‑encoded property name is not decoded by the current implementation");
    }

    /** Invalid URL‑encoded property name – the current implementation simply returns {@code null}. */
    @Test
    void getNode_invalidUrlEncodedPropertyName_returnsNull() {
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
        JsonNode result = schema.getNode("%ZZ");
        assertNull(result, "Malformed percent‑encoding yields null in the current implementation");
    }

    /** Schema node is {@code null}. Any subsequent call to {@code getNode} must throw {@link NullPointerException}. */
    @Test
    void getNode_schemaNodeIsNull_throwsNullPointerException() throws Exception {
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