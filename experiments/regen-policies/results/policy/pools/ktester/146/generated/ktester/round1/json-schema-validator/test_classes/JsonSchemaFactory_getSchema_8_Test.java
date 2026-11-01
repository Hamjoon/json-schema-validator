package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    /**
     * The factory under test.
     */
    private JsonSchemaFactory factory;

    /**
     * A simple JSON‑Schema used as input for the tests.
     */
    private JsonNode simpleSchemaNode;

    /**
     * Default configuration for validators.
     */
    private SchemaValidatorsConfig defaultConfig;

    /**
     * Jackson mapper for building {@link JsonNode} instances from literal JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a factory that uses the official Draft‑2020‑12 meta‑schema.
         * The builder method supplies a minimal set of loaders/mappers
         * (the defaults) and therefore does not need any explicit mocking.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            // No customisation required for the happy‑path tests.
        });
        // Create a minimal, valid JSON‑Schema node: {"type":"string"}
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"string\"}");
        // Use the default configuration (all flags set to their defaults)
        defaultConfig = SchemaValidatorsConfig.builder().build();
        /*
         * Use the 2020‑12 draft as the base meta‑schema.  The builder is left untouched
         * (no custom loaders/mappers) because the happy‑path tests do not need any
         * special behaviour.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            // No customisation required for the scenarios below.
        });
        // The library supplies a sensible default configuration.
        defaultConfig = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the factory holds no external resources.
        factory = null;
        simpleSchemaNode = null;
        defaultConfig = null;
        factory = null;
        defaultConfig = null;
    }

    @Test
    void getSchema_WithValidJsonNode_ReturnsNonNullJsonSchema() {
        // GIVEN a JsonSchemaFactory (setup in @BeforeEach) and a valid schema node
        // WHEN the factory is asked to produce a JsonSchema from the node
        JsonSchema schema = factory.getSchema(simpleSchemaNode, defaultConfig);
        // THEN a non‑null JsonSchema instance is returned
        assertNotNull(schema, "The factory should never return null for a valid schema node");
        // AND the schema's underlying node matches the one we supplied
        assertSame(simpleSchemaNode, schema.getSchemaNode(), "The JsonSchema should retain the original JsonNode supplied to the factory");
    }

    @Test
    void getSchema_WithEmptyObjectNode_ReturnsJsonSchema() {
        // GIVEN an empty object node (still a syntactically valid JSON‑Schema)
        JsonNode emptyNode = new ObjectMapper().createObjectNode();
        // WHEN the factory creates a JsonSchema
        JsonSchema schema = factory.getSchema(emptyNode, defaultConfig);
        // THEN the result is non‑null and contains the same node
        assertNotNull(schema);
        assertSame(emptyNode, schema.getSchemaNode());
    }

    @Test
    void getSchema_WithNullJsonNode_ThrowsNullPointerException() {
        // GIVEN a null JsonNode
        JsonNode nullNode = null;
        // WHEN the method is invoked, THEN a NPE is expected (the factory does not accept null)
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullNode, defaultConfig), "Calling getSchema with a null JsonNode should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithValidNodeAndConfig() throws Exception {
        // {"type":"object","properties":{"name":{"type":"string"}}}
        JsonNode node = MAPPER.readTree("{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }");
        // default values
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema(node, cfg);
        assertNotNull(schema, "Factory must return a non‑null JsonSchema");
        assertSame(node, schema.getSchemaNode(), "Returned JsonSchema should expose the original JsonNode");
        // default meta‑schema for V202012 is draft‑2020‑12; the test description mentions draft‑07,
        // but the factory is configured for V202012 – we simply verify that a meta‑schema is present.
        assertNotNull(schema.getMetaSchema(), "A meta‑schema should be attached to the created JsonSchema");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithValidNodeAndNullConfig() throws Exception {
        // {"type":"array","items":{"type":"integer"}}
        JsonNode node = MAPPER.readTree("{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }");
        // null config
        JsonSchema schema = factory.getSchema(node, null);
        assertNotNull(schema, "Factory must still produce a JsonSchema when config is null");
        assertSame(node, schema.getSchemaNode(), "The schema node inside the JsonSchema must be the one supplied");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithEmptyObjectNode() throws Exception {
        JsonNode emptyNode = MAPPER.readTree("{}");
        JsonSchema schema = factory.getSchema(emptyNode, defaultConfig);
        assertNotNull(schema, "Empty object node should still yield a JsonSchema");
        assertSame(emptyNode, schema.getSchemaNode(), "The schema node inside the JsonSchema must be the empty object we passed");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithNullJsonNode() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, defaultConfig), "Calling getSchema with a null JsonNode must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithInvalidSchemaNode() throws Exception {
        // {"type":"unknownType"}
        JsonNode node = MAPPER.readTree("{ \"type\": \"unknownType\" }");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node, defaultConfig), "An unknown type keyword should cause a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithMetaSchemaReference() throws Exception {
        // {"$schema":"http://json-schema.org/draft-04/schema#","type":"string"}
        JsonNode node = MAPPER.readTree("{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"string\" }");
        JsonSchema schema = factory.getSchema(node, defaultConfig);
        assertNotNull(schema, "Factory must return a JsonSchema for a schema that declares $schema");
        JsonMetaSchema meta = schema.getMetaSchema();
        assertNotNull(meta, "Meta‑schema must be resolved when $schema is present");
        // The meta‑schema URI for draft‑04 ends with ".../draft-04/schema#"
        assertTrue(meta.getUri().toString().contains("draft-04"), "The resolved meta‑schema should be draft‑04");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithNonObjectNode() throws Exception {
        // "\"just a string\""
        JsonNode node = MAPPER.readTree("\"just a string\"");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node, defaultConfig), "Root schema node that is not an object must cause a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithUnresolvableRef() throws Exception {
        // {"$ref":"http://nonexistent.example.com/schema.json"}
        JsonNode node = MAPPER.readTree("{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node, defaultConfig), "A $ref that cannot be fetched must result in a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithInvalidIdUri() throws Exception {
        // {"$id":"ht!tp://[invalid]","type":"object"}
        JsonNode node = MAPPER.readTree("{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }");
        // The library may throw IllegalArgumentException or JsonSchemaException – we accept both.
        assertThrows(RuntimeException.class, () -> factory.getSchema(node, defaultConfig), "A malformed $id URI must cause an exception during schema creation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSchemaWithCircularReference() throws Exception {
        // {"$id":"http://example.com/root","$ref":"http://example.com/root"}
        JsonNode node = MAPPER.readTree("{ \"$id\": \"http://example.com/root\", \"$ref\": \"http://example.com/root\" }");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node, defaultConfig), "A direct circular $ref must be detected and reported as a JsonSchemaException");
    }
}
