```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
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
 * <p>These tests cover the most important execution paths:
 * <ul>
 *   <li>valid JSON and YAML inputs</li>
 *   <li>edge‑cases such as empty streams, leading BOM/whitespace</li>
 *   <li>error handling – malformed content, I/O failures, duplicate keys, etc.</li>
 *   <li>large schemas and Unicode handling</li>
 * </ul>
 *
 * <p>The {@code factory} instance is created once for the whole test suite using the
 * public {@code JsonSchemaFactory.getInstance(...)} method – this guarantees that all
 * internal collaborators (mapper factories, loaders, …) are correctly wired.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Factory instance shared by all tests. */
    private static JsonSchemaFactory factory;

    /** A very small, valid JSON‑Schema used for the tests. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;

    /** Minimal valid YAML schema (equivalent to {@link #SIMPLE_SCHEMA}). */
    private static final String SIMPLE_YAML_SCHEMA = """
            $schema: "https://json-schema.org/draft/2020-12/schema"
            type: object
            properties:
              name:
                type: string
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // Build the factory with the default configuration for Draft‑2020‑12.
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> {
                    // No customisation needed for these tests.
                });
        assertNotNull(factory, "Factory should have been created");
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    /** Turns a {@link String} into an {@link InputStream} using UTF‑8. */
    private static InputStream asStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns an {@link InputStream} that starts with a UTF‑8 BOM. */
    private static InputStream withBom(String json) {
        byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + jsonBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(jsonBytes, 0, combined, bom.length, jsonBytes.length);
        return new ByteArrayInputStream(combined);
    }

    /** Generates a large JSON schema with {@code propertyCount} top‑level properties. */
    private static String largeSchema(int propertyCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < propertyCount; i++) {
            sb.append("\"p").append(i).append("\":{\"type\":\"string\"}");
            if (i < propertyCount - 1) sb.append(",");
        }
        sb.append("}}");
        return sb.toString();
    }

    /* --------------------------------------------------------------------- *
     *  Positive test cases (return a JsonSchema)
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_fromValidInputStream_returnsJsonSchema() {
        InputStream schemaStream = asStream(SIMPLE_SCHEMA);
        JsonSchema jsonSchema = factory.getSchema(schemaStream);
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object should be a JsonSchema");
    }

    @Test
    void getSchema_fromValidYamlInputStream_returnsJsonSchema() {
        InputStream yamlStream = asStream(SIMPLE_YAML_SCHEMA);
        JsonSchema jsonSchema = factory.getSchema(yamlStream);
        assertNotNull(jsonSchema, "JsonSchema should not be null for valid YAML");
    }

    @Test
    void getSchema_fromInputStreamWithUtf8Bom_parsesSuccessfully() {
        InputStream bomStream = withBom("{\"type\":\"object\"}");
        JsonSchema schema = factory.getSchema(bomStream);
        assertNotNull(schema, "Schema should be parsed even when a UTF‑8 BOM is present");
    }

    @Test
    void getSchema_fromInputStreamWithLeadingWhitespace_parsesSuccessfully() {
        String content = "\n  \t  {\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(asStream(content));
        assertNotNull(schema, "Schema should be parsed despite leading whitespace");
    }

    @Test
    void getSchema_fromLargeSchema_parsesWithoutError() {
        String large = largeSchema(1_000);
        JsonSchema schema = factory.getSchema(asStream(large));
        assertNotNull(schema, "Factory must handle a large schema (1 000 properties)");
    }

    @Test
    void getSchema_fromSchemaWithUnicodeProperty_preservesUnicodeName() {
        String json = """
                {
                  "type":"object",
                  "properties":{
                    "café":{"type":"string"}
                  }
                }
                """;
        JsonSchema schema = factory.getSchema(asStream(json));
        assertNotNull(schema);
        // The underlying JsonNode can be inspected via the public API.
        JsonNode root = schema.getSchemaNode();
        assertNotNull(root);
        JsonNode props = root.get("properties");
        assertNotNull(props);
        assertTrue(props.has("café"), "Unicode property name should be preserved");
    }

    @Test
    void getSchema_fromSchemaWithDuplicateKeys_lastKeyWins() {
        String json = """
                {
                  "type":"object",
                  "type":"string"
                }
                """;
        JsonSchema schema = factory.getSchema(asStream(json));
        assertNotNull(schema);
        JsonNode root = schema.getSchemaNode();
        assertEquals("string", root.get("type").asText(),
                "When duplicate keys exist, the last occurrence should win");
    }

    @Test
    void getSchema_fromSchemaWithDraft07_detectsCorrectMetaSchema() {
        String json = """
                {
                  "$schema":"http://json-schema.org/draft-07/schema#",
                  "type":"object"
                }
                """;
        JsonSchema schema = factory.getSchema(asStream(json));
        assertNotNull(schema);
        // JsonSchema exposes its meta‑schema via getMetaSchema()
        JsonMetaSchema meta = schema.getMetaSchema();
        assertEquals(VersionFlag.V7, meta.getSpecVersion(),
                "Factory should pick Draft‑07 when $schema points to it");
    }

    @Test
    void getSchema_fromSchemaWithUnknown$schema_fallsBackToDefault() {
        String json = """
                {
                  "$schema":"http://example.com/unknown/schema#",
                  "type":"object"
                }
                """;
        JsonSchema schema = factory.getSchema(asStream(json));
        assertNotNull(schema);
        JsonMetaSchema meta = schema.getMetaSchema();
        // The default for this factory is Draft‑2020‑12 (V202012)
        assertEquals(VersionFlag.V202012, meta.getSpecVersion(),
                "Unknown $schema URI should fall back to the factory's default version");
    }

    /* --------------------------------------------------------------------- *
     *  Negative test cases (exceptions)
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_fromNullInputStream_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((InputStream) null),
                "Calling getSchema(null) must throw NullPointerException");
    }

    @Test
    void getSchema_fromEmptyInputStream_throwsIllegalArgumentException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(empty),
                "Empty InputStream should result in IllegalArgumentException");
    }

    @Test
    void getSchema_fromMalformedJson_throwsJsonSchemaException() {
        String malformed = "{ \"type\" : \"object\" "; // missing closing brace
        InputStream stream = asStream(malformed);
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream),
                "Malformed JSON must cause JsonSchemaException");
    }

    @Test
    void getSchema_fromMalformedYaml_throwsJsonSchemaException() {
        String malformedYaml = "type: [object"; // invalid YAML
        InputStream stream = asStream(malformedYaml);
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream),
                "Malformed YAML must cause JsonSchemaException");
    }

    @Test
    void getSchema_whenReadingThrowsIOException_propagatesException(@Mock InputStream failingStream) throws IOException {
        // Simulate a stream that throws IOException on any read operation.
        when(failingStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated I/O failure"));
        when(failingStream.read()).thenThrow(new IOException("simulated I/O failure"));
        assertThrows(IOException.class,
                () -> factory.getSchema(failingStream),
                "IOException from the underlying stream must be propagated");
    }

    @Test
    void getSchema_fromSchemaWithInvalidId_throwsIllegalArgumentException() {
        String json = """
                {
                  "$id":"ht!tp://[invalid]",
                  "type":"object"
                }
                """;
        InputStream stream = asStream(json);
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(stream),
                "Invalid $id URI should cause IllegalArgumentException");
    }

    @Test
    void getSchema_fromUnrecognizableContent_throwsJsonSchemaException() {
        String randomText = "Just some random text that is neither JSON nor YAML";
        InputStream stream = asStream(randomText);
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream),
                "Content that cannot be parsed as JSON/YAML must raise JsonSchemaException");
    }

    @Test
    void getSchema_fromClosedInputStream_throwsIOException() throws IOException {
        InputStream closed = mock(InputStream.class);
        when(closed.read(any())).thenThrow(new IOException("stream closed"));
        when(closed.read()).thenThrow(new IOException("stream closed"));
        assertThrows(IOException.class,
                () -> factory.getSchema(closed),
                "Reading from a closed InputStream must propagate IOException");
    }

    @Test
    void getSchema_fromSchemaWithCircularRef_throwsJsonSchemaException() {
        String json = """
                {
                  "$defs":{
                    "a":{"$ref":"#/$defs/b"},
                    "b":{"$ref":"#/$defs/a"}
                  },
                  "type":"object",
                  "properties":{
                    "c":{"$ref":"#/$defs/a"}
                  }
                }
                """;
        InputStream stream = asStream(json);
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream),
                "Circular $ref should cause JsonSchemaException (or similar)");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests for a few simple positive scenarios
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(
                Arguments.of("Simple JSON", SIMPLE_SCHEMA),
                Arguments.of("Simple YAML", SIMPLE_YAML_SCHEMA),
                Arguments.of("JSON with leading whitespace", "\n  \t  {\"type\":\"object\"}"),
                Arguments.of("JSON with UTF‑8 BOM", new String(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF}) + "{\"type\":\"object\"}")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validSchemaProvider")
    void getSchema_parameterised_validInputs_returnNonNull(String description, String content) {
        JsonSchema schema = factory.getSchema(asStream(content));
        assertNotNull(schema, description + " should produce a non‑null JsonSchema");
    }
}
```