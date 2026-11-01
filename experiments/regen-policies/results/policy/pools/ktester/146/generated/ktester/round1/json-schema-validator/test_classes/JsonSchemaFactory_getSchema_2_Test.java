package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.io.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /**
     * Factory instance used in the tests.
     */
    private JsonSchemaFactory factory;

    /**
     * Simple JSON schema used for the happy‑path test.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": [ "name" ]
        }
        """;

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a factory instance for the 2020‑12 draft. The static factory method
        // builds a fully‑initialised JsonSchemaFactory with all default dependencies,
        // which is sufficient for exercising getSchema(InputStream).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Helper that builds an InputStream from a literal string.
     */
    private static InputStream streamOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    static Stream<Arguments> validJsonSchemas() {
        return Stream.of(Arguments.of(streamOf("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), "object", "name"), Arguments.of(streamOf("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"array\"}"), "array", // we only check the meta‑schema version for this case
        null));
    }

    static Stream<InputStream> emptyOrWhitespaceStreams() {
        return Stream.of(streamOf("   "), new ByteArrayInputStream(new byte[0]));
    }

    static Stream<InputStream> malformedContentStreams() {
        return Stream.of(// malformed JSON
        streamOf("{invalid json}"), // malformed YAML
        streamOf("type: [unbalanced"));
    }

    /**
     * Generates a JSON schema with the given number of simple integer properties.
     */
    private static String generateLargeSchema(int propertyCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < propertyCount; i++) {
            sb.append("\"prop").append(i).append("\":{\"type\":\"integer\"}");
            if (i < propertyCount - 1)
                sb.append(",");
        }
        sb.append("}}");
        return sb.toString();
    }

    @Test
    void getSchema_withValidInputStream_returnsJsonSchema() {
        // GIVEN a valid InputStream containing a JSON schema
        InputStream schemaStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));
        // WHEN the factory parses the stream
        JsonSchema jsonSchema = factory.getSchema(schemaStream);
        // THEN a non‑null JsonSchema is returned and its meta‑schema matches the draft version
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // The meta‑schema IRI for draft‑2020‑12 is defined in JsonMetaSchema; we can verify it
        JsonMetaSchema meta = jsonSchema.getMetaSchema();
        assertNotNull(meta, "Meta schema should not be null");
        assertEquals("https://json-schema.org/draft/2020-12/schema", meta.getUri(), "Meta schema IRI should correspond to the 2020‑12 draft");
    }

    @Test
    void getSchema_withNullInputStream_throwsNullPointerException() {
        // GIVEN a null InputStream
        InputStream nullStream = null;
        // WHEN getSchema is invoked
        // THEN a NullPointerException (or IllegalArgumentException) is expected.
        // The actual implementation throws NPE when trying to read from the stream,
        // so we assert that any NPE is thrown.
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullStream));
    }

    @ParameterizedTest(name = "[{index}] valid JSON schema")
    @MethodSource("validJsonSchemas")
    void getSchema_validJsonSchemas_parsesCorrectly(InputStream schemaStream, String expectedRootType, String expectedProperty) {
        JsonSchema schema = factory.getSchema(schemaStream);
        assertNotNull(schema, "Returned JsonSchema must not be null");
        // Verify root type (OBJECT, ARRAY, …) via the underlying JsonNode
        JsonNode rootNode = schema.getSchemaNode();
        assertEquals(expectedRootType, rootNode.get("type").asText(), "Root type should match expected");
        // If a property name is expected, verify its presence
        if (expectedProperty != null) {
            assertTrue(rootNode.path("properties").has(expectedProperty), "Schema should contain property '" + expectedProperty + "'");
        }
        // For the second case we also assert that the meta‑schema is Draft‑07
        if (expectedRootType.equals("array")) {
            JsonMetaSchema meta = schema.getMetaSchema();
            assertNotNull(meta);
            assertEquals("http://json-schema.org/draft-07/schema#", meta.getUri(), "Meta‑schema IRI should correspond to Draft‑07");
        }
    }

    @Test
    void getSchema_validYamlSchema_parsesCorrectly() {
        // YAML representation of a simple object schema
        String yaml = """
            type: object
            properties:
              age:
                type: integer
            """;
        InputStream yamlStream = streamOf(yaml);
        JsonSchema schema = factory.getSchema(yamlStream);
        assertNotNull(schema, "Returned JsonSchema must not be null");
        JsonNode root = schema.getSchemaNode();
        assertEquals("object", root.get("type").asText(), "Root type should be OBJECT");
        assertTrue(root.path("properties").has("age"), "Property 'age' must be present");
        assertEquals("integer", root.path("properties").path("age").get("type").asText(), "Property 'age' should be of type integer");
    }

    @ParameterizedTest(name = "[{index}] empty/whitespace input")
    @MethodSource("emptyOrWhitespaceStreams")
    void getSchema_emptyOrWhitespace_throwsIllegalArgumentException(InputStream emptyStream) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(emptyStream));
        assertTrue(ex.getMessage().toLowerCase().contains("schema content is empty"), "Exception message should mention empty schema content");
    }

    @ParameterizedTest(name = "[{index}] malformed content")
    @MethodSource("malformedContentStreams")
    void getSchema_malformedContent_throwsIOException(InputStream malformedStream) {
        // The implementation uses JsonNodeReader which throws IOException on parse errors.
        assertThrows(IOException.class, () -> factory.getSchema(malformedStream));
    }

    @Test
    void getSchema_ioErrorDuringRead_propagatesIOException() throws IOException {
        // Mock an InputStream that throws IOException on the first read()
        InputStream broken = mock(InputStream.class);
        when(broken.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated read error"));
        IOException ex = assertThrows(IOException.class, () -> factory.getSchema(broken));
        assertEquals("simulated read error", ex.getMessage(), "Exception message must match the simulated error");
    }

    @Test
    void getSchema_utf8Bom_isSkippedAndParsedSuccessfully() {
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        String json = "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}";
        byte[] content = new byte[bom.length + json.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(bom, 0, content, 0, bom.length);
        System.arraycopy(json.getBytes(StandardCharsets.UTF_8), 0, content, bom.length, json.getBytes(StandardCharsets.UTF_8).length);
        InputStream stream = new ByteArrayInputStream(content);
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema must be parsed despite the BOM");
        JsonNode root = schema.getSchemaNode();
        assertEquals("object", root.get("type").asText());
        assertTrue(root.path("properties").has("age"));
    }

    @Test
    void getSchema_unsupportedSchemaVersion_throwsIllegalArgumentException() {
        // The library does not support the (hypothetical) draft‑2020‑12# (note the trailing '#')
        String schema = """
            {
              "$schema":"http://json-schema.org/draft-2020-12/schema#",
              "type":"object"
            }
            """;
        InputStream stream = streamOf(schema);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(stream));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported $schema"), "Exception message should mention unsupported $schema");
    }

    @Test
    void getSchema_largeSchema_parsesWithoutOutOfMemoryError() {
        String largeSchema = generateLargeSchema(10_000);
        InputStream stream = streamOf(largeSchema);
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Large schema should be parsed successfully");
        JsonNode root = schema.getSchemaNode();
        assertEquals("object", root.get("type").asText());
        assertEquals(10_000, root.path("properties").size(), "Root should contain the expected number of properties");
    }

    @Test
    void getSchema_internalRef_isResolvedToString() {
        String schema = """
            {
              "$ref":"#/definitions/name",
              "definitions":{"name":{"type":"string"}}
            }
            """;
        InputStream stream = streamOf(schema);
        JsonSchema jsonSchema = factory.getSchema(stream);
        assertNotNull(jsonSchema, "Schema with $ref must be parsed");
        // After resolution the effective root node should have type "string"
        JsonNode resolved = jsonSchema.getSchemaNode();
        assertEquals("string", resolved.get("type").asText(), "Resolved $ref should result in type STRING");
    }

    @Test
    void getSchema_withId_setsSchemaLocation() {
        String schema = """
            {
              "$id":"http://example.com/schemas/person",
              "type":"object",
              "properties":{"firstName":{"type":"string"}}
            }
            """;
        InputStream stream = streamOf(schema);
        JsonSchema jsonSchema = factory.getSchema(stream);
        assertNotNull(jsonSchema, "Schema containing $id must be parsed");
        // Verify that the $id is present in the underlying JsonNode
        JsonNode root = jsonSchema.getSchemaNode();
        assertTrue(root.has("$id"), "Root node should contain $id");
        assertEquals("http://example.com/schemas/person", root.get("$id").asText(), "$id value should be preserved");
    }
}
