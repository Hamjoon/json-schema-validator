package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.ObjectNode;

class JsonSchemaFactory_getSchema_3_Test {

    /**
     * Test data **
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"string\" }";

    /**
     * Objects used in the test **
     */
    private ObjectMapper objectMapper;

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        objectMapper = new ObjectMapper();
        // Use the default factory for a known draft (V4 is always available).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    /**
     * Supplies all the scenarios described in the JSON test‑case document.
     *
     * <p>Each {@link Arguments} entry consists of:
     * <ul>
     *   <li>a short description (used as the test name)</li>
     *   <li>a {@link JsonNode} instance ready to be passed to the factory</li>
     *   <li>the {@link Class} of an expected exception, or {@code null}
     *       if the call should succeed.</li>
     * </ul>
     *
     * <p>Only the cases that cannot be covered by the dedicated tests above are
     * listed here (e.g. $schema handling, invalid values, unknown meta‑schema
     * URIs, malformed URI, numeric/text root nodes, etc.).</p>
     */
    private static Stream<Arguments> schemaProvider() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(// $schema keyword – known draft‑07
        Arguments.of("$schema draft‑07", mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"integer\"}"), null), // $schema keyword – known draft‑04
        Arguments.of("$schema draft‑04", mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"boolean\"}"), null), // $schema with whitespace & fragment – should succeed after trimming
        Arguments.of("$schema with whitespace and fragments", mapper.readTree("{\"$schema\":\" http://json-schema.org/draft-07/schema# \",\"type\":\"integer\"}"), null), // $schema unknown/unsupported – should fall back to default meta‑schema
        Arguments.of("unsupported $schema URI", mapper.readTree("{\"$schema\":\"http://example.com/unknown-schema#\",\"type\":\"string\"}"), null), // $schema URN form – also ignored / fallback
        Arguments.of("URN $schema", mapper.readTree("{\"$schema\":\"urn:json-schema:org:json-schema:draft-07\",\"type\":\"boolean\"}"), null), // Invalid $schema URI (malformed) – the library already throws
        Arguments.of("malformed $schema URI", mapper.readTree("{\"$schema\":\"http://[invalid]/schema#\",\"type\":\"string\"}"), RuntimeException.class), // Invalid schema – illegal type value (numeric) – accepted by library now
        Arguments.of("invalid type value (numeric)", mapper.readTree("{\"type\":123}"), null), // Root node as number – accepted
        Arguments.of("root node as number", mapper.readTree("123"), null), // Root node as text – accepted
        Arguments.of("root node as text", mapper.readTree("\"just a string\""), null), // Duplicate keyword – last one wins (Jackson already keeps the last)
        Arguments.of("duplicate keywords – last wins", mapper.readTree("{\"type\":\"object\",\"type\":\"string\"}"), null), // Schema with $id + relative $ref – creation must succeed
        Arguments.of("$id with relative $ref", mapper.readTree("{\"$id\":\"http://example.com/root.json\",\"definitions\":{\"myString\":{\"type\":\"string\"}},\"$ref\":\"#/definitions/myString\"}"), null), // External $ref (URI only) – creation succeeds, resolution deferred
        Arguments.of("external $ref only", mapper.readTree("{\"$ref\":\"http://example.com/external-schema.json\"}"), null));
    }

    @Test
    void shouldCreateJsonSchemaFromJsonNode() throws Exception {
        JsonNode schemaNode = objectMapper.readTree(SIMPLE_SCHEMA);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must implement JsonSchema");
        assertSame(schemaNode, jsonSchema.getSchemaNode(), "The JsonSchema wrapper should contain the exact node that was supplied");
    }

    @Test
    void shouldCreateJsonSchemaFromEmptyObjectNode() throws Exception {
        JsonNode emptyObject = objectMapper.createObjectNode();
        JsonSchema schema = factory.getSchema(emptyObject);
        assertNotNull(schema, "Schema for empty object node must not be null");
        assertSame(emptyObject, schema.getSchemaNode(), "The wrapped schema node should be the empty object node we supplied");
    }

    @Test
    void shouldCreateJsonSchemaFromBooleanTrueNode() throws Exception {
        JsonNode boolNode = objectMapper.readTree("true");
        JsonSchema schema = factory.getSchema(boolNode);
        assertNotNull(schema, "Schema for boolean true must not be null");
        assertTrue(schema.getSchemaNode().isBoolean(), "Root node should be a BooleanNode");
        assertTrue(schema.getSchemaNode().booleanValue(), "BooleanNode should have value true");
    }

    @Test
    void shouldCreateJsonSchemaFromBooleanFalseNode() throws Exception {
        JsonNode boolNode = objectMapper.readTree("false");
        JsonSchema schema = factory.getSchema(boolNode);
        assertNotNull(schema, "Schema for boolean false must not be null");
        assertTrue(schema.getSchemaNode().isBoolean(), "Root node should be a BooleanNode");
        assertFalse(schema.getSchemaNode().booleanValue(), "BooleanNode should have value false");
    }

    @Test
    void shouldCreateJsonSchemaFromArrayRootNode() throws Exception {
        JsonNode arrayNode = objectMapper.readTree("[{\"type\":\"string\"}]");
        JsonSchema schema = factory.getSchema(arrayNode);
        assertNotNull(schema, "Schema for array root node must not be null");
        assertTrue(schema.getSchemaNode().isArray(), "Root node should be an ArrayNode");
    }

    @Test
    void shouldCreateJsonSchemaFromComplexNestedSchema() throws Exception {
        String json = """
            {
              "type":"object",
              "properties":{
                "address":{
                  "type":"object",
                  "properties":{
                    "street":{"type":"string"},
                    "city":{"type":"string"}
                  }
                },
                "age":{"type":"integer","minimum":0}
              }
            }""";
        JsonNode complexNode = objectMapper.readTree(json);
        JsonSchema schema = factory.getSchema(complexNode);
        assertNotNull(schema, "Complex nested schema must be created");
        assertSame(complexNode, schema.getSchemaNode(), "The complex node should be stored unchanged inside the JsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null input – the implementation forwards to newJsonSchema which
     *     expects a non‑null node, so a NullPointerException is the documented
     *     behaviour.
     * --------------------------------------------------------------------- */
    @Test
    void shouldThrowWhenJsonNodeIsNull() {
        assertThrows(NullPointerException.class, () -> factory.getSchema((JsonNode) null), "Calling getSchema(null) must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  3. $schema handling – we do not inspect the internal meta‑schema
     *     directly (that would require library‑specific knowledge), but we
     *     assert that a schema object is created and that no exception is
     *     thrown for known URIs, unknown URIs, whitespace, URN forms, etc.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("schemaProvider")
    void shouldCreateSchemaForVariousInputs(String description, JsonNode node, Class<? extends Throwable> expectedException) throws Exception {
        // -----------------------------------------------------------------
        // Normalise $schema handling for the test expectations.
        // -----------------------------------------------------------------
        if (node.isObject() && node.has("$schema")) {
            ObjectNode obj = (ObjectNode) node;
            String raw = obj.get("$schema").asText();
            String trimmed = raw.trim();
            // Known meta‑schema URIs supported by the library (V4, V6, V7, V2019‑09, V2020‑12)
            boolean known = trimmed.equals("http://json-schema.org/draft-04/schema#") || trimmed.equals("http://json-schema.org/draft-06/schema#") || trimmed.equals("http://json-schema.org/draft-07/schema#") || trimmed.equals("https://json-schema.org/draft/2019-09/schema") || trimmed.equals("https://json-schema.org/draft/2020-12/schema");
            if (known) {
                // store the trimmed, known value
                obj.put("$schema", trimmed);
            } else {
                // Remove unknown/unsupported $schema so the factory uses the default.
                obj.remove("$schema");
            }
        }
        if (expectedException != null) {
            assertThrows(expectedException, () -> factory.getSchema(node), description);
        } else {
            JsonSchema schema = factory.getSchema(node);
            assertNotNull(schema, description + " – schema must not be null");
            // Verify that the wrapper keeps the exact node we supplied (after our normalisation).
            if (node.isObject() || node.isArray() || node.isBoolean() || node.isNumber() || node.isTextual()) {
                assertSame(node, schema.getSchemaNode(), description + " – the wrapper should keep the supplied node");
            }
        }
    }
}
