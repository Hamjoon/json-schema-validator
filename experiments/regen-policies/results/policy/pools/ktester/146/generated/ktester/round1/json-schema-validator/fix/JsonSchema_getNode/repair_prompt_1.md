
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
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    private static JsonSchemaFactory factory;

    private JsonSchema objectSchema;

    private JsonSchema arraySchema;

    private Method getNodeMethod;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the latest draft (2020‑12) – the version does not affect the getNode logic.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getNode_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getNode_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validateUnknownFormat(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: \r\n     * When the Format-Assertion vocabulary is specified, implementations MUST fail upon encountering unknown formats.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @param node the node\r\n     * @param rootNode the root node\r\n     * @param instanceLocation the instance location\r\n     * @return the messages\r\n     ;\n\t\tmethod `protected Object getAnnotationValue()` returns `java.lang.Object`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: \r\n     * Gets the annotation value.\r\n     * \r\n     * @return the annotation value\r\n     ;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


