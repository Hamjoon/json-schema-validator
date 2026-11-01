```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    /** Factory used for creating {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

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

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise per‑test at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
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
}
```