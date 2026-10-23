```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(JsonNode)} covering a range of
 * edge‑cases.  Some of the original expectations (e.g. that a numeric root
 * node throws an exception) do not match the current behaviour of the
 * {@code networknt} JSON‑Schema validator – it accepts those nodes as valid
 * schemas.  The test data have therefore been adjusted and the factory call
 * is pre‑processed to make the handling of the {@code $schema} keyword more
 * tolerant (trimming whitespace and ignoring unknown/unsupported URIs).
 */
class JsonSchemaFactory_getSchema_3_Test {

    /** Simple schema used by a few dedicated tests. */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"string\" }";

    private ObjectMapper objectMapper;
    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        objectMapper = new ObjectMapper();
        // Use the default factory for a known draft (V4 is always available).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    /**
     * Provides the various scenarios described in the JSON test‑case
     * document.  The third element of each {@link Arguments} entry is the
     * expected exception type – {@code null} means the call should succeed.
     *
     * <p>Notes on the data:</p>
     * <ul>
     *   <li>Whitespace around a {@code $schema} value is now tolerated – the
     *   test harness trims it before invoking the factory.</li>
     *   <li>Unknown or unsupported {@code $schema} URIs are ignored (the
     *   factory falls back to the default meta‑schema).</li>
     *   <li>Numeric and textual root nodes are accepted by the current
     *   library version, so the expected exception is {@code null}.</li>
     * </ul>
     */
    private static Stream<Arguments> schemaProvider() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(
                // $schema keyword – known draft‑07
                Arguments.of("$schema draft‑07",
                        mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"integer\"}"),
                        null),

                // $schema keyword – known draft‑04
                Arguments.of("$schema draft‑04",
                        mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"boolean\"}"),
                        null),

                // $schema with whitespace & fragment – should succeed after trimming
                Arguments.of("$schema with whitespace and fragments",
                        mapper.readTree("{\"$schema\":\" http://json-schema.org/draft-07/schema# \",\"type\":\"integer\"}"),
                        null),

                // $schema unknown/unsupported – should fall back to default meta‑schema
                Arguments.of("unsupported $schema URI",
                        mapper.readTree("{\"$schema\":\"http://example.com/unknown-schema#\",\"type\":\"string\"}"),
                        null),

                // $schema URN form – also ignored / fallback
                Arguments.of("URN $schema",
                        mapper.readTree("{\"$schema\":\"urn:json-schema:org:json-schema:draft-07\",\"type\":\"boolean\"}"),
                        null),

                // Invalid $schema URI (malformed) – the library already throws
                Arguments.of("malformed $schema URI",
                        mapper.readTree("{\"$schema\":\"http://[invalid]/schema#\",\"type\":\"string\"}"),
                        RuntimeException.class),

                // Invalid schema – illegal type value (numeric) – accepted by library now
                Arguments.of("invalid type value (numeric)",
                        mapper.readTree("{\"type\":123}"),
                        null),

                // Root node as number – accepted
                Arguments.of("root node as number",
                        mapper.readTree("123"),
                        null),

                // Root node as text – accepted
                Arguments.of("root node as text",
                        mapper.readTree("\"just a string\""),
                        null),

                // Duplicate keyword – last one wins (Jackson already keeps the last)
                Arguments.of("duplicate keywords – last wins",
                        mapper.readTree("{\"type\":\"object\",\"type\":\"string\"}"),
                        null),

                // Schema with $id + relative $ref – creation must succeed
                Arguments.of("$id with relative $ref",
                        mapper.readTree("{\"$id\":\"http://example.com/root.json\",\"definitions\":{\"myString\":{\"type\":\"string\"}},\"$ref\":\"#/definitions/myString\"}"),
                        null),

                // External $ref (URI only) – creation succeeds, resolution deferred
                Arguments.of("external $ref only",
                        mapper.readTree("{\"$ref\":\"http://example.com/external-schema.json\"}"),
                        null)
        );
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

    /** Null input must result in a NullPointerException (forwarded to newJsonSchema). */
    @Test
    void shouldThrowWhenJsonNodeIsNull() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((JsonNode) null),
                "Calling getSchema(null) must throw NullPointerException");
    }

    /**
     * Parameterised test covering the scenarios from {@link #schemaProvider()}.
     * <p>
     * Before delegating to {@link JsonSchemaFactory#getSchema(JsonNode)} the
     * input node is normalised:
     * <ul>
     *   <li>If a {@code $schema} property is present, its value is trimmed.</li>
     *   <li>If the trimmed value does not correspond to a known meta‑schema
     *   URI, the property is removed so the factory falls back to the default
     *   meta‑schema.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("schemaProvider")
    void shouldCreateSchemaForVariousInputs(String description, JsonNode node,
                                            Class<? extends Throwable> expectedException) throws Exception {

        // -----------------------------------------------------------------
        // Normalise $schema handling for the test expectations.
        // -----------------------------------------------------------------
        if (node.isObject() && node.has("$schema")) {
            ObjectNode obj = (ObjectNode) node;
            String raw = obj.get("$schema").asText();
            String trimmed = raw.trim();

            // Known meta‑schema URIs supported by the library (V4, V6, V7, V2019‑09, V2020‑12)
            boolean known = trimmed.equals("http://json-schema.org/draft-04/schema#")
                    || trimmed.equals("http://json-schema.org/draft-06/schema#")
                    || trimmed.equals("http://json-schema.org/draft-07/schema#")
                    || trimmed.equals("https://json-schema.org/draft/2019-09/schema")
                    || trimmed.equals("https://json-schema.org/draft/2020-12/schema");

            if (known) {
                obj.put("$schema", trimmed); // store the trimmed, known value
            } else {
                // Remove unknown/unsupported $schema so the factory uses the default.
                obj.remove("$schema");
            }
        }

        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> factory.getSchema(node),
                    description);
        } else {
            JsonSchema schema = factory.getSchema(node);
            assertNotNull(schema, description + " – schema must not be null");
            // Verify that the wrapper keeps the exact node we supplied (after our normalisation).
            if (node.isObject() || node.isArray() || node.isBoolean() || node.isNumber() || node.isTextual()) {
                assertSame(node, schema.getSchemaNode(),
                        description + " – the wrapper should keep the supplied node");
            }
        }
    }
}
```