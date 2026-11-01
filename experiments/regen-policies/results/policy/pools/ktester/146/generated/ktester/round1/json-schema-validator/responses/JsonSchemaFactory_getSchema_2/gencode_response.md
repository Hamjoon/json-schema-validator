```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream)}.
 *
 * The test suite covers the scenarios described in the JSON test‑case definition,
 * using JUnit 5 and Mockito.  Each logical group is exercised by a dedicated test
 * (or a parameterised test when several inputs share the same expectations).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Factory instance used in the tests. */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for the happy‑path test. */
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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a factory instance for the 2020‑12 draft.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* --------------------------------------------------------------------- */
    /*  Existing simple tests (kept unchanged)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchema_withValidInputStream_returnsJsonSchema() {
        // GIVEN a valid InputStream containing a JSON schema
        InputStream schemaStream = new ByteArrayInputStream(
                SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));

        // WHEN the factory parses the stream
        JsonSchema jsonSchema = factory.getSchema(schemaStream);

        // THEN a non‑null JsonSchema is returned and its meta‑schema matches the draft version
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        JsonMetaSchema meta = jsonSchema.getMetaSchema();
        assertNotNull(meta, "Meta schema should not be null");
        assertEquals("https://json-schema.org/draft/2020-12/schema", meta.getUri(),
                "Meta schema IRI should correspond to the 2020‑12 draft");
    }

    @Test
    void getSchema_withNullInputStream_throwsNullPointerException() {
        // GIVEN a null InputStream
        InputStream nullStream = null;

        // WHEN getSchema is invoked
        // THEN a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullStream));
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for the various groups described in the JSON     */
    /* --------------------------------------------------------------------- */

    /** Helper that builds an InputStream from a literal string. */
    private static InputStream streamOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /* ------------------- Valid JSON schema parsing ---------------------- */

    static Stream<Arguments> validJsonSchemas() {
        return Stream.of(
                Arguments.of(
                        streamOf("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"),
                        "object",
                        "name"
                ),
                Arguments.of(
                        streamOf("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"array\"}"),
                        "array",
                        null // we only check the meta‑schema version for this case
                )
        );
    }

    @ParameterizedTest(name = "[{index}] valid JSON schema")
    @MethodSource("validJsonSchemas")
    void getSchema_validJsonSchemas_parsesCorrectly(InputStream schemaStream,
                                                    String expectedRootType,
                                                    String expectedProperty) {
        JsonSchema schema = factory.getSchema(schemaStream);
        assertNotNull(schema, "Returned JsonSchema must not be null");

        // Verify root type (OBJECT, ARRAY, …) via the underlying JsonNode
        JsonNode rootNode = schema.getSchemaNode();
        assertEquals(expectedRootType, rootNode.get("type").asText(),
                "Root type should match expected");

        // If a property name is expected, verify its presence
        if (expectedProperty != null) {
            assertTrue(rootNode.path("properties").has(expectedProperty),
                    "Schema should contain property '" + expectedProperty + "'");
        }

        // For the second case we also assert that the meta‑schema is Draft‑07
        if (expectedRootType.equals("array")) {
            JsonMetaSchema meta = schema.getMetaSchema();
            assertNotNull(meta);
            assertEquals("http://json-schema.org/draft-07/schema#", meta.getUri(),
                    "Meta‑schema IRI should correspond to Draft‑07");
        }
    }

    /* ------------------- Valid YAML schema parsing ---------------------- */

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
        assertEquals("integer", root.path("properties").path("age").get("type").asText(),
                "Property 'age' should be of type integer");
    }

    /* ------------------- Empty or whitespace only input ----------------- */

    static Stream<InputStream> emptyOrWhitespaceStreams() {
        return Stream.of(
                streamOf("   "),
                new ByteArrayInputStream(new byte[0])
        );
    }

    @ParameterizedTest(name = "[{index}] empty/whitespace input")
    @MethodSource("emptyOrWhitespaceStreams")
    void getSchema_emptyOrWhitespace_throwsIllegalArgumentException(InputStream emptyStream) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(emptyStream));
        assertTrue(ex.getMessage().toLowerCase().contains("schema content is empty"),
                "Exception message should mention empty schema content");
    }

    /* ------------------- Invalid JSON/YAML content ---------------------- */

    static Stream<InputStream> malformedContentStreams() {
        return Stream.of(
                // malformed JSON
                streamOf("{invalid json}"),
                // malformed YAML
                streamOf("type: [unbalanced")
        );
    }

    @ParameterizedTest(name = "[{index}] malformed content")
    @MethodSource("malformedContentStreams")
    void getSchema_malformedContent_throwsIOException(InputStream malformedStream) {
        // The implementation uses JsonNodeReader which throws IOException on parse errors.
        assertThrows(IOException.class, () -> factory.getSchema(malformedStream));
    }

    /* ------------------- IO error while reading stream ------------------- */

    @Test
    void getSchema_ioErrorDuringRead_propagatesIOException() throws IOException {
        // Mock an InputStream that throws IOException on the first read()
        InputStream broken = mock(InputStream.class);
        when(broken.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated read error"));

        IOException ex = assertThrows(IOException.class, () -> factory.getSchema(broken));
        assertEquals("simulated read error", ex.getMessage(),
                "Exception message must match the simulated error");
    }

    /* ------------------- UTF‑8 BOM handling ----------------------------- */

    @Test
    void getSchema_utf8Bom_isSkippedAndParsedSuccessfully() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
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

    /* ------------------- Unsupported $schema version -------------------- */

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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(stream));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported $schema"),
                "Exception message should mention unsupported $schema");
    }

    /* ------------------- Large schema handling -------------------------- */

    @Test
    void getSchema_largeSchema_parsesWithoutOutOfMemoryError() {
        String largeSchema = generateLargeSchema(10_000);
        InputStream stream = streamOf(largeSchema);
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Large schema should be parsed successfully");
        JsonNode root = schema.getSchemaNode();
        assertEquals("object", root.get("type").asText());
        assertEquals(10_000, root.path("properties").size(),
                "Root should contain the expected number of properties");
    }

    /** Generates a JSON schema with the given number of simple integer properties. */
    private static String generateLargeSchema(int propertyCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < propertyCount; i++) {
            sb.append("\"prop").append(i).append("\":{\"type\":\"integer\"}");
            if (i < propertyCount - 1) sb.append(",");
        }
        sb.append("}}");
        return sb.toString();
    }

    /* ------------------- Internal $ref resolution ---------------------- */

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
        assertEquals("string", resolved.get("type").asText(),
                "Resolved $ref should result in type STRING");
    }

    /* ------------------- Schema with $id URI ---------------------------- */

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
        assertEquals("http://example.com/schemas/person", root.get("$id").asText(),
                "$id value should be preserved");
    }
}
```