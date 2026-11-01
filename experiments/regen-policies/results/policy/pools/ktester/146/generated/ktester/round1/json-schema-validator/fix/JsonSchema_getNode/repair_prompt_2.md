


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
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.Field;

// @ExtendWith(MockitoExtension.class)
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

17:33:37.390 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.401 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.403 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.404 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.407 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.408 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.409 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.410 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.411 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.411 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.413 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.414 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.415 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.415 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.416 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.418 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.419 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.419 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.420 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.421 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.422 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.422 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.423 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
17:33:37.423 [main] WARN com.networknt.schema.JsonMetaSchema -- Could not load validator items
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_getNode_Test ✔
│     ├─ getNode_booleanArgument_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_arrayNegativeIndex_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_arrayIndexIntegerWithinBounds_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_WithPropertyName_ReturnsCorrespondingNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_numberOnObjectNode_missingKey_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_urlEncodedPlus_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_emptyStringKey_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_WithInvalidKey_ReturnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_characterArgument_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_malformedUrlEncoding_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_propertyExists_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_stringNonNumericOnArray_throwsNumberFormatException() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_nullInternalSchemaNode_throwsNPE() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_arrayIndexDoubleCoerced_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_unicodeUrlEncodedKey_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_numberOnObjectNode_treatedAsStringKey() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_nullArgument_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_propertyMissing_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_WithInvalidIndex_ReturnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_onPrimitiveNode_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_urlEncodedSpace_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_WithArrayIndex_ReturnsCorrespondingItemNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     ├─ getNode_arrayIndexOutOfBounds_returnsNull() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
│     └─ getNode_arrayIndexLongCoerced_returnsNode() ✘ java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (24):
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_booleanArgument_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_booleanArgument_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_arrayNegativeIndex_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_arrayNegativeIndex_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_arrayIndexIntegerWithinBounds_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_arrayIndexIntegerWithinBounds_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_WithPropertyName_ReturnsCorrespondingNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_WithPropertyName_ReturnsCorrespondingNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_numberOnObjectNode_missingKey_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_numberOnObjectNode_missingKey_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_urlEncodedPlus_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_urlEncodedPlus_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_emptyStringKey_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_emptyStringKey_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_WithInvalidKey_ReturnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_WithInvalidKey_ReturnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_characterArgument_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_characterArgument_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_malformedUrlEncoding_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_malformedUrlEncoding_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_propertyExists_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_propertyExists_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_stringNonNumericOnArray_throwsNumberFormatException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_stringNonNumericOnArray_throwsNumberFormatException', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_nullInternalSchemaNode_throwsNPE()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_nullInternalSchemaNode_throwsNPE', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_arrayIndexDoubleCoerced_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_arrayIndexDoubleCoerced_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_unicodeUrlEncodedKey_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_unicodeUrlEncodedKey_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_numberOnObjectNode_treatedAsStringKey()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_numberOnObjectNode_treatedAsStringKey', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_nullArgument_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_nullArgument_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_propertyMissing_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_propertyMissing_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_WithInvalidIndex_ReturnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_WithInvalidIndex_ReturnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_onPrimitiveNode_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_onPrimitiveNode_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_urlEncodedSpace_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_urlEncodedSpace_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_WithArrayIndex_ReturnsCorrespondingItemNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_WithArrayIndex_ReturnsCorrespondingItemNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_arrayIndexOutOfBounds_returnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_arrayIndexOutOfBounds_returnsNull', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]
  JUnit Jupiter:JsonSchema_getNode_Test:getNode_arrayIndexLongCoerced_returnsNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_Test', methodName = 'getNode_arrayIndexLongCoerced_returnsNode', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:508)
       com.networknt.schema.ValidationContext.newValidator(ValidationContext.java:63)
       com.networknt.schema.JsonSchema.read(JsonSchema.java:542)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1316)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       [...]
     Caused by: java.lang.IllegalArgumentException: The value of 'items' MUST be a valid JSON Schema.
       com.networknt.schema.ItemsValidator202012.<init>(ItemsValidator202012.java:59)
       com.networknt.schema.ValidatorTypeCode.newValidator(ValidatorTypeCode.java:162)
       com.networknt.schema.JsonMetaSchema.newValidator(JsonMetaSchema.java:496)
       [...]

Test run finished after 375 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        24 tests found           ]
[         0 tests skipped         ]
[        24 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[        24 tests failed          ]


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
 

return type: 
com.fasterxml.jackson.databind.JsonNode

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;
	
class `com.networknt.schema.FormatValidator`:
	related functions:
		method `protected Set<ValidationMessage> validateUnknownFormat(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: 
     * When the Format-Assertion vocabulary is specified, implementations MUST fail upon encountering unknown formats.
     * 
     * @param executionContext the execution context
     * @param node the node
     * @param rootNode the root node
     * @param instanceLocation the instance location
     * @return the messages
     ;
		method `protected Object getAnnotationValue()` returns `java.lang.Object`, related with `com.networknt.schema.JsonSchema.getNode(Object)`, api document: 
     * Gets the annotation value.
     * 
     * @return the annotation value
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getNode(Object)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
