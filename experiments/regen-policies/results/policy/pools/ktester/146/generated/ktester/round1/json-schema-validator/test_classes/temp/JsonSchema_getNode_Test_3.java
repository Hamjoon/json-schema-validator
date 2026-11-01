package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.Field;

class JsonSchema_getNode_Test {

    private static JsonSchemaFactory factory;

    private JsonSchema objectSchema;

    private JsonSchema arraySchema;

    private Method getNodeMethod;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the draft‑07 (2019‑09) meta‑schema – it supports an 'items' array.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---------- Object schema ----------
        String objectSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              }
            }""";
        objectSchema = factory.getSchema(objectSchemaJson);
        // ---------- Array schema ----------
        String arraySchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "array",
              "items": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }""";
        arraySchema = factory.getSchema(arraySchemaJson);
        // Obtain the protected method once (shared for both schemas)
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

    @Test
    void getNode_WithPropertyName_ReturnsCorrespondingNode() throws Exception {
        JsonNode propertiesNode = (JsonNode) getNodeMethod.invoke(objectSchema, "properties");
        assertNotNull(propertiesNode, "properties node should not be null");
        assertTrue(propertiesNode.isObject(), "properties node should be an ObjectNode");
        ObjectNode objNode = (ObjectNode) propertiesNode;
        assertTrue(objNode.has("name"), "properties should contain 'name'");
        assertTrue(objNode.has("age"), "properties should contain 'age'");
        JsonNode nameDef = objNode.get("name");
        JsonNode ageDef = objNode.get("age");
        assertEquals("string", nameDef.get("type").asText(), "name type should be string");
        assertEquals("integer", ageDef.get("type").asText(), "age type should be integer");
    }

    @Test
    void getNode_WithArrayIndex_ReturnsCorrespondingItemNode() throws Exception {
        JsonNode firstItemNode = (JsonNode) getNodeMethod.invoke(arraySchema, 0);
        assertNotNull(firstItemNode, "first item node should not be null");
        assertTrue(firstItemNode.isObject(), "first item node should be an ObjectNode");
        assertEquals("string", firstItemNode.get("type").asText(), "first item type should be string");
        JsonNode secondItemNode = (JsonNode) getNodeMethod.invoke(arraySchema, 1);
        assertNotNull(secondItemNode, "second item node should not be null");
        assertTrue(secondItemNode.isObject(), "second item node should be an ObjectNode");
        assertEquals("integer", secondItemNode.get("type").asText(), "second item type should be integer");
    }

    @Test
    void getNode_WithInvalidKey_ReturnsNull() throws Exception {
        JsonNode missingNode = (JsonNode) getNodeMethod.invoke(objectSchema, "nonExisting");
        assertNull(missingNode, "Missing property should yield a null node");
    }

    @Test
    void getNode_WithInvalidIndex_ReturnsNull() throws Exception {
        JsonNode outOfBounds = (JsonNode) getNodeMethod.invoke(arraySchema, 5);
        assertNull(outOfBounds, "Out‑of‑bounds index should yield a null node");
    }

    /* -------------------------------------------------------------
     *  Property name (String) – existing field
     * ------------------------------------------------------------- */
    @Test
    void getNode_propertyExists_returnsNode() throws Exception {
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
        ObjectNode root = mapper.createObjectNode().put("漢字", "漢字");
        JsonSchema schema = schemaWithNode(root);
        String encoded = "%E6%BC%A2%E5%AD%97";
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
        ArrayNode array = mapper.createArrayNode().add(10).add(20).add(30);
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
        ArrayNode array = mapper.createArrayNode().add(10).add(20);
        JsonSchema schema = schemaWithNode(array);
        JsonNode result = (JsonNode) getNodeMethod.invoke(schema, 5);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  Array index – Long coerced to int
     * ------------------------------------------------------------- */
    @Test
    void getNode_arrayIndexLongCoerced_returnsNode() throws Exception {
        ArrayNode array = mapper.createArrayNode().add(10).add(20).add(30);
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
        ArrayNode array = mapper.createArrayNode().add(10).add(20).add(30);
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
        ArrayNode array = mapper.createArrayNode().add(10).add(20);
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
        JsonSchema schema = schemaWithNode(mapper.createObjectNode());
        Field schemaNodeField = BaseJsonValidator.class.getDeclaredField("schemaNode");
        schemaNodeField.setAccessible(true);
        schemaNodeField.set(schema, null);
        assertThrows(NullPointerException.class, () -> getNodeMethod.invoke(schema, "any"));
    }

    /* -------------------------------------------------------------
     *  String index on ArrayNode that cannot be parsed – NumberFormatException
     * ------------------------------------------------------------- */
    @Test
    void getNode_stringNonNumericOnArray_throwsNumberFormatException() throws Exception {
        ArrayNode array = mapper.createArrayNode().add(10).add(20);
        JsonSchema schema = schemaWithNode(array);
        assertThrows(NumberFormatException.class, () -> getNodeMethod.invoke(schema, "abc"));
    }
}
