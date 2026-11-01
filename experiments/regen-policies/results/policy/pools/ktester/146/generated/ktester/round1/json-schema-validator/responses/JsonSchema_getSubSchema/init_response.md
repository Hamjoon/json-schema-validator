```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

class JsonSchema_getSubSchema_Test {

    /** Factory used for creating real {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** ObjectMapper for turning JSON strings into {@link JsonNode}s. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A simple JSON‑Schema used by the tests. */
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

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the current tests.
    }

    /** Helper that creates a {@link JsonSchema} from the constant {@link #SIMPLE_SCHEMA}. */
    private JsonSchema createRootSchema() {
        return factory.getSchema(SIMPLE_SCHEMA);
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
        assertEquals("string", subNode.get("type").asText(),
                "The subschema for '/properties/name' must have type 'string'");
    }

    @Test
    void getSubSchema_nonExistingProperty_throwsInvalidSchemaRefException() {
        // Arrange – root schema and a fragment that does **not** exist.
        JsonSchema root = createRootSchema();
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/unknown");

        // Act & Assert – the method must throw the expected exception.
        assertThrows(InvalidSchemaRefException.class,
                () -> root.getSubSchema(fragment),
                "Requesting a non‑existent fragment should result in InvalidSchemaRefException");
    }
}
```