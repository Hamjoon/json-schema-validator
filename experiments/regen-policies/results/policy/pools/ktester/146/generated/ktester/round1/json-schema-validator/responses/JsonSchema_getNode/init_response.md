```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

/**
 * Unit tests for the protected {@code getNode(Object)} method of {@link JsonSchema}.
 *
 * <p>The tests obtain a {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * then use reflection to invoke the protected method. Two scenarios are covered:
 *
 * <ul>
 *   <li>Retrieving a child node by property name from an object schema.</li>
 *   <li>Retrieving an element by index from an array schema.</li>
 * </ul>
 *
 * The tests verify that the returned {@link JsonNode} matches the expected
 * structure of the original schema definition.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema objectSchema;
    private JsonSchema arraySchema;
    private Method getNodeMethod;

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

    @AfterEach
    void teardownAfterEach() {
        // No resources to release; the schemas are lightweight.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    @Test
    void getNode_WithPropertyName_ReturnsCorrespondingNode() throws Exception {
        // Act: retrieve the "properties" node from the object schema
        JsonNode propertiesNode = (JsonNode) getNodeMethod.invoke(objectSchema, "properties");

        // Assert: the node must be an object and contain the expected fields
        assertNotNull(propertiesNode, "properties node should not be null");
        assertTrue(propertiesNode.isObject(), "properties node should be an ObjectNode");

        ObjectNode objNode = (ObjectNode) propertiesNode;
        assertTrue(objNode.has("name"), "properties should contain 'name'");
        assertTrue(objNode.has("age"), "properties should contain 'age'");

        // Verify the inner definitions
        JsonNode nameDef = objNode.get("name");
        JsonNode ageDef = objNode.get("age");
        assertEquals("string", nameDef.get("type").asText(), "name type should be string");
        assertEquals("integer", ageDef.get("type").asText(), "age type should be integer");
    }

    @Test
    void getNode_WithArrayIndex_ReturnsCorrespondingItemNode() throws Exception {
        // Act: retrieve the first item (index 0) from the array schema
        JsonNode firstItemNode = (JsonNode) getNodeMethod.invoke(arraySchema, 0);

        // Assert: the node must be an object describing a string type
        assertNotNull(firstItemNode, "first item node should not be null");
        assertTrue(firstItemNode.isObject(), "first item node should be an ObjectNode");
        assertEquals("string", firstItemNode.get("type").asText(), "first item type should be string");

        // Act: retrieve the second item (index 1)
        JsonNode secondItemNode = (JsonNode) getNodeMethod.invoke(arraySchema, 1);

        // Assert: the node must describe an integer type
        assertNotNull(secondItemNode, "second item node should not be null");
        assertTrue(secondItemNode.isObject(), "second item node should be an ObjectNode");
        assertEquals("integer", secondItemNode.get("type").asText(), "second item type should be integer");
    }

    @Test
    void getNode_WithInvalidKey_ReturnsNull() throws Exception {
        // Act: request a non‑existing property
        JsonNode missingNode = (JsonNode) getNodeMethod.invoke(objectSchema, "nonExisting");

        // Assert: the method should return null (Jackson returns null for missing fields)
        assertNull(missingNode, "Missing property should yield a null node");
    }

    @Test
    void getNode_WithInvalidIndex_ReturnsNull() throws Exception {
        // Act: request an out‑of‑bounds index on the array schema
        JsonNode outOfBounds = (JsonNode) getNodeMethod.invoke(arraySchema, 5);

        // Assert: should be null because the index does not exist
        assertNull(outOfBounds, "Out‑of‑bounds index should yield a null node");
    }
}
```