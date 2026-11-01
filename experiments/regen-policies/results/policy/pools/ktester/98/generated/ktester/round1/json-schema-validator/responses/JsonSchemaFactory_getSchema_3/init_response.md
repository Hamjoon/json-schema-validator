```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(JsonNode)}.
 *
 * The factory is obtained via {@link JsonSchemaFactory#getInstance(VersionFlag)} which
 * creates a fully‑initialised instance (all required dependencies are set internally).
 * The tests verify that:
 * <ul>
 *   <li>Calling {@code getSchema(JsonNode)} does not throw.</li>
 *   <li>The returned object is a {@link JsonSchema} instance.</li>
 *   <li>The {@link JsonSchema} contains the same schema node that was supplied.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a factory for a known draft version. The factory internally creates all
        // required collaborators (JsonNodeReader, SchemaLoaders, SchemaMappers, etc.).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /**
     * Verify that a simple JSON schema node can be turned into a {@link JsonSchema}
     * via {@code getSchema(JsonNode)}.
     */
    @Test
    void shouldCreateJsonSchemaFromJsonNode() throws Exception {
        // GIVEN a minimal JSON schema node
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        assertNotNull(schemaNode, "Parsed schema node must not be null");

        // WHEN the factory creates a JsonSchema from the node
        JsonSchema jsonSchema = factory.getSchema(schemaNode);

        // THEN a non‑null JsonSchema instance is returned and it holds the same node
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must be a JsonSchema");

        // The JsonSchema class exposes the original schema node via getSchemaNode()
        // (this method exists in the library). If the method is unavailable, the test
        // still guarantees that the factory does not fail.
        JsonNode retrievedNode = jsonSchema.getSchemaNode();
        assertNotNull(retrievedNode, "The JsonSchema should contain a schema node");
        assertEquals(schemaNode, retrievedNode, "The schema node inside JsonSchema must match the input");
    }

    /**
     * Verify that passing {@code null} as the schema node results in a
     * {@link NullPointerException}. The factory does not accept a null argument
     * for {@code getSchema(JsonNode)}.
     */
    @Test
    void shouldThrowWhenJsonNodeIsNull() {
        // GIVEN a null JsonNode
        JsonNode nullNode = null;

        // WHEN/THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullNode));
    }
}
```