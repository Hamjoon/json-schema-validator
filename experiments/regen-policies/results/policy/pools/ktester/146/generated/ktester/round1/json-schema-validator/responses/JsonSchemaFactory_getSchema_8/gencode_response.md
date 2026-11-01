```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The tests cover the scenarios described in the supplied JSON test‑case list:
 * <ul>
 *   <li>valid schema node with explicit config</li>
 *   <li>valid schema node with {@code null} config (factory must fall back to defaults)</li>
 *   <li>empty object node</li>
 *   <li>null {@code JsonNode}</li>
 *   <li>invalid/unknown type keyword</li>
 *   <li>explicit $schema reference (draft‑04)</li>
 *   <li>root node that is not an object</li>
 *   <li>unresolvable $ref</li>
 *   <li>malformed $id URI</li>
 *   <li>circular $ref</li>
 * </ul>
 *
 * <p>All tests use the public {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, java.util.function.Consumer)}
 * factory method – no reflection on private members is required.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    /** Factory under test – created for each test method. */
    private JsonSchemaFactory factory;

    /** Default configuration used when a test does not supply its own. */
    private SchemaValidatorsConfig defaultConfig;

    /** Jackson mapper for building {@link JsonNode} instances from literal JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        /*
         * Use the 2020‑12 draft as the base meta‑schema.  The builder is left untouched
         * (no custom loaders/mappers) because the happy‑path tests do not need any
         * special behaviour.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> {
                    // No customisation required for the scenarios below.
                });

        // The library supplies a sensible default configuration.
        defaultConfig = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void tearDown() {
        factory = null;
        defaultConfig = null;
    }

    /* --------------------------------------------------------------------- */
    /* 1. Valid node + explicit default configuration                         */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithValidNodeAndConfig() throws Exception {
        // {"type":"object","properties":{"name":{"type":"string"}}}
        JsonNode node = MAPPER.readTree(
                "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }");

        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig(); // default values

        JsonSchema schema = factory.getSchema(node, cfg);

        assertNotNull(schema, "Factory must return a non‑null JsonSchema");
        assertSame(node, schema.getSchemaNode(),
                "Returned JsonSchema should expose the original JsonNode");
        // default meta‑schema for V202012 is draft‑2020‑12; the test description mentions draft‑07,
        // but the factory is configured for V202012 – we simply verify that a meta‑schema is present.
        assertNotNull(schema.getMetaSchema(),
                "A meta‑schema should be attached to the created JsonSchema");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Valid node + null configuration (factory must fall back)           */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithValidNodeAndNullConfig() throws Exception {
        // {"type":"array","items":{"type":"integer"}}
        JsonNode node = MAPPER.readTree("{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }");

        JsonSchema schema = factory.getSchema(node, null); // null config

        assertNotNull(schema, "Factory must still produce a JsonSchema when config is null");
        assertSame(node, schema.getSchemaNode(),
                "The schema node inside the JsonSchema must be the one supplied");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Empty object node ({} – valid schema that matches anything)        */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithEmptyObjectNode() throws Exception {
        JsonNode emptyNode = MAPPER.readTree("{}");

        JsonSchema schema = factory.getSchema(emptyNode, defaultConfig);

        assertNotNull(schema, "Empty object node should still yield a JsonSchema");
        assertSame(emptyNode, schema.getSchemaNode(),
                "The schema node inside the JsonSchema must be the empty object we passed");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Null JsonNode → NullPointerException                               */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithNullJsonNode() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, defaultConfig),
                "Calling getSchema with a null JsonNode must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Invalid/unknown type keyword → JsonSchemaException                  */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithInvalidSchemaNode() throws Exception {
        // {"type":"unknownType"}
        JsonNode node = MAPPER.readTree("{ \"type\": \"unknownType\" }");

        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(node, defaultConfig),
                "An unknown type keyword should cause a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Explicit $schema reference (draft‑04) → meta‑schema detection        */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithMetaSchemaReference() throws Exception {
        // {"$schema":"http://json-schema.org/draft-04/schema#","type":"string"}
        JsonNode node = MAPPER.readTree(
                "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"string\" }");

        JsonSchema schema = factory.getSchema(node, defaultConfig);

        assertNotNull(schema, "Factory must return a JsonSchema for a schema that declares $schema");
        JsonMetaSchema meta = schema.getMetaSchema();
        assertNotNull(meta, "Meta‑schema must be resolved when $schema is present");
        // The meta‑schema URI for draft‑04 ends with ".../draft-04/schema#"
        assertTrue(meta.getUri().toString().contains("draft-04"),
                "The resolved meta‑schema should be draft‑04");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Root node not an object (e.g. a JSON string) → exception            */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithNonObjectNode() throws Exception {
        // "\"just a string\""
        JsonNode node = MAPPER.readTree("\"just a string\"");

        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(node, defaultConfig),
                "Root schema node that is not an object must cause a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Unresolvable $ref → JsonSchemaException                              */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithUnresolvableRef() throws Exception {
        // {"$ref":"http://nonexistent.example.com/schema.json"}
        JsonNode node = MAPPER.readTree("{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }");

        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(node, defaultConfig),
                "A $ref that cannot be fetched must result in a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Malformed $id URI → IllegalArgumentException (or JsonSchemaException) */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithInvalidIdUri() throws Exception {
        // {"$id":"ht!tp://[invalid]","type":"object"}
        JsonNode node = MAPPER.readTree("{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }");

        // The library may throw IllegalArgumentException or JsonSchemaException – we accept both.
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(node, defaultConfig),
                "A malformed $id URI must cause an exception during schema creation");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Circular $ref (schema refers to itself) → JsonSchemaException     */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithCircularReference() throws Exception {
        // {"$id":"http://example.com/root","$ref":"http://example.com/root"}
        JsonNode node = MAPPER.readTree(
                "{ \"$id\": \"http://example.com/root\", \"$ref\": \"http://example.com/root\" }");

        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(node, defaultConfig),
                "A direct circular $ref must be detected and reported as a JsonSchemaException");
    }
}
```