```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for the protected {@code getNode(Object)} method of {@link JsonSchema}.
 *
 * <p>The tests cover a wide range of scenarios described in the supplied JSON test
 * cases, including property look‑ups, URL‑decoding, numeric index handling, and error
 * conditions.  Because {@code getNode} is protected we use reflection to invoke it.
 *
 * <p>For many tests we need a {@link JsonSchema} whose internal {@code schemaNode}
 * is a specific {@link JsonNode} (object, array, primitive, or {@code null}).  The
 * {@link JsonSchemaFactory} always creates a schema with a non‑null node, so we
 * replace the protected {@code BaseJsonValidator.schemaNode} field via reflection.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    private static JsonSchemaFactory factory;
    private Method getNodeMethod;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        // Use the latest draft – the version does not affect getNode logic.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        // Obtain the protected method once.
        getNodeMethod = JsonSchema.class.getDeclaredMethod("getNode", Object.class);
        getNodeMethod.setAccessible(true);
    }

    /**
     * Helper that creates a {@link JsonSchema} instance and overwrites its internal
     * {@code schemaNode} with the supplied {@link JsonNode}.
     */
    private JsonSchema schemaWithNode(JsonNode node) throws Exception {
        // Create a dummy schema (empty object) and then replace its schemaNode.
        JsonSchema dummy = factory.getSchema("{}");
        Field schemaNodeField = BaseJsonValidator.class.getDeclaredField("schemaNode");
        schemaNodeField.setAccessible(true);
        schemaNodeField.set(dummy, node);
        return dummy;
    }

    /* -------------------------------------------------------------
     *  Property name (String) – existing field
     * ------------------------------------------------------------- */
    @Test
    void getNode_propertyExists_returnsNode() throws Exception {
        // {"name":"John"}
        ObjectNode root = mapper.createObjectNode().put("name", "John");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "name");
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertEquals("John", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Property name (String) – does NOT exist
     * ------------------------------------------------------------- */
    @Test
    void getNode_propertyMissing_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("present", "value");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "missing");
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Property name – URL‑encoded (space as %20)
     * ------------------------------------------------------------- */
    @Test
    void getNode_urlEncodedSpace_returnsNode() throws Exception {
        // field name contains a space: "first name":"John"
        ObjectNode root = mapper.createObjectNode().put("first name", "John");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "first%20name");
        assertNotNull(result);
        assertEquals("John", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Property name – URL‑encoded with plus sign (decoded to space)
     * ------------------------------------------------------------- */
    @Test
    void getNode_urlEncodedPlus_returnsNode() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("first name", "John");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "first+name");
        assertNotNull(result);
        assertEquals("John", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Property name – malformed percent‑encoding
     * ------------------------------------------------------------- */
    @Test
    void getNode_malformedUrlEncoding_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("bad%2G", "value");
        JsonSchema schema = schemaWithNode(root);

        // The method should safely ignore the malformed sequence and return null.
        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "bad%2G");
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Property name – empty string key
     * ------------------------------------------------------------- */
    @Test
    void getNode_emptyStringKey_returnsNode() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("", "empty");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "");
        assertNotNull(result);
        assertEquals("empty", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Property name – Unicode key URL‑decoded
     * ------------------------------------------------------------- */
    @Test
    void getNode_unicodeUrlEncodedKey_returnsNode() throws Exception {
        // field name is the Chinese characters "漢字" with value "漢字"
        ObjectNode root = mapper.createObjectNode().put("漢字", "漢字");
        JsonSchema schema = schemaWithNode(root);

        String encoded = "%E6%BC%A2%E5%AD%97"; // "漢字"
        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, encoded);
        assertNotNull(result);
        assertEquals("漢字", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Property name – null argument
     * ------------------------------------------------------------- */
    @Test
    void getNode_nullArgument_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("any", "value");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, (Object) null);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Property name – unsupported type (Boolean)
     * ------------------------------------------------------------- */
    @Test
    void getNode_booleanArgument_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("any", "value");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, true);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Property name – unsupported type (Character)
     * ------------------------------------------------------------- */
    @Test
    void getNode_characterArgument_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("any", "value");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 'c');
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Property name – node is a primitive (text) – should return null
     * ------------------------------------------------------------- */
    @Test
    void getNode_onPrimitiveNode_returnsNull() throws Exception {
        TextNode primitive = new TextNode("just text");
        JsonSchema schema = schemaWithNode(primitive);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, "any");
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Array index – within bounds (Integer)
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayIndexIntegerWithinBounds_returnsNode() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20)
                .add(30);
        JsonSchema schema = schemaWithNode(array);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 1);
        assertNotNull(result);
        assertTrue(result.isNumber());
        assertEquals(20, result.intValue());
    }

    /* -------------------------------------------------------------
     *  Array index – out of bounds
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayIndexOutOfBounds_returnsNull() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20);
        JsonSchema schema = schemaWithNode(array);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 5);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Array index – Long coerced to int
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayIndexLongCoerced_returnsNode() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20)
                .add(30);
        JsonSchema schema = schemaWithNode(array);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 2L);
        assertNotNull(result);
        assertEquals(30, result.intValue());
    }

    /* -------------------------------------------------------------
     *  Array index – Double coerced via Number.intValue()
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayIndexDoubleCoerced_returnsNode() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20)
                .add(30);
        JsonSchema schema = schemaWithNode(array);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 1.9);
        assertNotNull(result);
        assertEquals(20, result.intValue());
    }

    /* -------------------------------------------------------------
     *  Array index – negative index
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayNegativeIndex_returnsNull() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20);
        JsonSchema schema = schemaWithNode(array);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, -1);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Number used on ObjectNode – treated as String key
     * ------------------------------------------------------------- */
    @Test
    void getNode_numberOnObjectNode_treatedAsStringKey() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("2", "two");
        JsonSchema schema = schemaWithNode(root);

        // The method will convert the Number to a String ("2") and look it up.
        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 2);
        assertNotNull(result);
        assertEquals("two", result.textValue());
    }

    /* -------------------------------------------------------------
     *  Number used on ObjectNode – key does not exist
     * ------------------------------------------------------------- */
    @Test
    void getNode_numberOnObjectNode_missingKey_returnsNull() throws Exception {
        ObjectNode root = mapper.createObjectNode().put("one", "1");
        JsonSchema schema = schemaWithNode(root);

        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 2);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Null internal schemaNode – should throw NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void getNode_nullInternalSchemaNode_throwsNPE() throws Exception {
        JsonSchema schema = schemaWithNode(mapper.createObjectNode()); // start with non‑null
        // Replace the internal node with null via reflection.
        Field schemaNodeField = BaseJsonValidator.class.getDeclaredField("schemaNode");
        schemaNodeField.setAccessible(true);
        schemaNodeField.set(schema, null);

        assertThrows(NullPointerException.class,
                () -> getNodeMethod.invoke(schema, "any"));
    }

    /* -------------------------------------------------------------
     *  String index on ArrayNode that cannot be parsed – NumberFormatException
     * ------------------------------------------------------------- */
    @Test
    void getNode_stringNonNumericOnArray_throwsNumberFormatException() throws Exception {
        ArrayNode array = mapper.createArrayNode()
                .add(10)
                .add(20);
        JsonSchema schema = schemaWithNode(array);

        // The method attempts Integer.parseInt and propagates the exception.
        assertThrows(NumberFormatException.class,
                () -> getNodeMethod.invoke(schema, "abc"));
    }
}
```