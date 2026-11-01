package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    /**
     * Factory used for all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Jackson mapper used to create {@link JsonNode} instances.
     */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version is not important for the
        // behaviour we are testing (creation of a JsonSchema from a URI + JsonNode).
        mapper = new ObjectMapper();
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    private static Stream<Arguments> validInputsProvider() {
        return Stream.of(// simple object schema
        Arguments.of(URI.create("http://example.com/schema.json"), JsonMapperFactory.getMapper().readTree("{\"type\":\"object\"}"), "http://example.com/schema.json", "simple object schema"), // empty string URI
        Arguments.of(URI.create(""), JsonMapperFactory.getMapper().readTree("{\"type\":\"string\"}"), "", "empty string URI"), // encoded space in URI
        Arguments.of(URI.create("file:/tmp/space%20in%20name.json"), JsonMapperFactory.getMapper().readTree("{\"type\":\"array\",\"items\":{\"type\":\"number\"}}"), "file:/tmp/space in name.json", "URI with encoded spaces"), // URN with $schema keyword
        Arguments.of(URI.create("urn:example:schema"), JsonMapperFactory.getMapper().readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"null\"}"), "urn:example:schema", "URN with $schema"));
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(Arguments.of(null, JsonMapperFactory.getMapper().readTree("{\"type\":\"object\"}"), "null URI"), Arguments.of(URI.create("http://example.com/schema.json"), null, "null JsonNode"), Arguments.of(null, null, "both null"));
    }

    /**
     * Verifies that {@code getSchema(URI, JsonNode)} returns a non‑null
     * {@link JsonSchema} whose location matches the supplied {@link URI}.
     */
    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN – a simple JSON schema and a URI that identifies it
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;
        JsonNode schemaNode = mapper.readTree(schemaJson);
        URI schemaUri = new URI("http://example.com/my-schema.json");
        // WHEN – the factory creates a JsonSchema from the URI and the JsonNode
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);
        // THEN – the result is a valid JsonSchema instance
        assertNotNull(jsonSchema, "The factory should return a non‑null JsonSchema");
        // The location stored inside the JsonSchema must be the same as the supplied URI
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(), "Schema location should match the supplied URI");
    }

    @ParameterizedTest(name = "[{index}] Valid input – {3}")
    @MethodSource("validInputsProvider")
    void getSchema_validInputs_returnsJsonSchema(URI uri, JsonNode node, String expectedLocation, String description) {
        JsonSchema schema = factory.getSchema(uri, node);
        assertNotNull(schema, "Schema should not be null for " + description);
        assertEquals(expectedLocation, schema.getSchemaLocation().toString(), "Location mismatch for " + description);
        // For the URN case we also check that the meta‑schema is Draft‑07
        if (uri.toString().startsWith("urn:")) {
            assertEquals("http://json-schema.org/draft-07/schema#", schema.getMetaSchema().getUri(), "Meta‑schema should be Draft‑07 for URN case");
        }
    }

    @ParameterizedTest(name = "[{index}] Null argument – {2}")
    @MethodSource("nullArgumentsProvider")
    void getSchema_nullArguments_throwsNPE(URI uri, JsonNode node, String description) {
        assertThrows(NullPointerException.class, () -> factory.getSchema(uri, node), "Expected NullPointerException for " + description);
    }

    @Test
    void getSchema_malformedUri_throwsIllegalArgumentException() {
        // URI.create throws IllegalArgumentException for malformed URIs
        URI malformed = URI.create("ht!tp://[invalid]");
        JsonNode node = JsonMapperFactory.getMapper().readTree("{\"type\":\"object\"}");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(malformed, node), "Malformed URI should result in IllegalArgumentException");
    }

    @Test
    void getSchema_complexSchema_withDefinitionsAndRef() throws IOException {
        URI uri = URI.create("http://example.com/complex.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("""
            {
              "$schema":"http://json-schema.org/draft-07/schema#",
              "definitions":{
                "positive":{"type":"integer","minimum":1}
              },
              "type":"object",
              "properties":{
                "age":{"$ref":"#/definitions/positive"}
              }
            }
            """);
        JsonSchema schema = factory.getSchema(uri, node);
        assertNotNull(schema, "Schema should be created");
        assertEquals(uri.toString(), schema.getSchemaLocation().toString(), "Location must match the supplied URI");
        assertEquals("http://json-schema.org/draft-07/schema#", schema.getMetaSchema().getUri(), "Meta‑schema should be Draft‑07");
        // Verify that the $ref is present in the parsed schema node (resolution is performed later)
        JsonNode properties = schema.getSchemaNode().path("properties");
        assertTrue(properties.has("age"), "Properties should contain 'age'");
        assertEquals("$ref", properties.get("age").fieldNames().next(), "$ref field should be present for 'age'");
    }

    @Test
    void getSchema_uriWithQueryAndFragment_preservesIt() throws IOException {
        URI uri = URI.create("https://example.org/schema.json?version=1#section2");
        JsonNode node = JsonMapperFactory.getMapper().readTree("{\"type\":\"string\",\"maxLength\":10}");
        JsonSchema schema = factory.getSchema(uri, node);
        assertEquals(uri.toString(), schema.getSchemaLocation().toString(), "Query and fragment must be preserved in the SchemaLocation");
    }

    @Test
    void getSchema_uriWithUnicodeCharacters_isRetained() throws IOException {
        // The Unicode character is kept after the factory normalises the URI
        URI uri = URI.create("http://example.com/über.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("{\"type\":\"null\"}");
        JsonSchema schema = factory.getSchema(uri, node);
        assertEquals(uri.toString(), schema.getSchemaLocation().toString(), "Unicode characters should survive normalization");
    }

    @Test
    void getSchema_booleanTrue_returnsTrueSchema() throws IOException {
        URI uri = URI.create("http://example.com/alwaysValid.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("true");
        JsonSchema schema = factory.getSchema(uri, node);
        assertTrue(schema.getSchemaNode().isBoolean(), "Schema node should be a boolean");
        assertTrue(schema.getSchemaNode().booleanValue(), "Boolean schema should be true");
    }

    @Test
    void getSchema_booleanFalse_returnsFalseSchema() throws IOException {
        URI uri = URI.create("http://example.com/alwaysInvalid.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("false");
        JsonSchema schema = factory.getSchema(uri, node);
        assertTrue(schema.getSchemaNode().isBoolean(), "Schema node should be a boolean");
        assertFalse(schema.getSchemaNode().booleanValue(), "Boolean schema should be false");
    }

    @Test
    void getSchema_stringLiteralNode_throwsIllegalArgumentException() throws IOException {
        URI uri = URI.create("http://example.com/invalidSchema.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("\"just a string\"");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(uri, node), "String literal as top‑level schema must be rejected");
    }

    @Test
    void getSchema_unknownMetaSchema_fallsBackToDefault() throws IOException {
        URI uri = URI.create("http://example.com/unknownMeta.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("""
            {
              "$schema":"http://example.com/unknown#",
              "type":"number"
            }
            """);
        JsonSchema schema = factory.getSchema(uri, node);
        assertNotNull(schema, "Schema should be created even with unknown $schema IRI");
        // The library falls back to its default (the version used to obtain the factory)
        String expectedDefault = factory.getInstance(SpecVersion.VersionFlag.V202012).getMetaSchema("http://json-schema.org/draft/2020-12/schema").getUri();
        assertEquals(expectedDefault, schema.getMetaSchema().getUri(), "Meta‑schema should fall back to the factory's default version");
    }

    @Test
    void getSchema_numericLiteralNode_throwsIllegalArgumentException() throws IOException {
        URI uri = URI.create("http://example.com/numberNode.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("5");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(uri, node), "Numeric literal as top‑level schema must be rejected");
    }

    @Test
    void getSchema_arrayLiteralNode_throwsIllegalArgumentException() throws IOException {
        URI uri = URI.create("http://example.com/arrayNode.json");
        JsonNode node = JsonMapperFactory.getMapper().readTree("[1,2,3]");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(uri, node), "Array literal as top‑level schema must be rejected");
    }
}
