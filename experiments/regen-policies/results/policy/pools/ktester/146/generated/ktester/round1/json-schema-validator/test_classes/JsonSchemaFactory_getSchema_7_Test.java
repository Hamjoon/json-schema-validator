package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /**
     * Factory shared by all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Simple JSON schema used in the happy‑path test.
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

    /**
     * Minimal valid JSON schema used in many tests.
     */
    private static final String MINIMAL_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": [ "name" ]
        }
        """;

    /**
     * Schema that expects an integer – used for config‑related validation tests.
     */
    private static final String INTEGER_PROPERTY_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "age": { "type": "integer" }
          },
          "required": [ "age" ]
        }
        """;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the latest draft version; the builder is not needed for this simple test.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static factory reference.
        factory = null;
    }

    private static Stream<Arguments> invalidInputProvider() {
        // Empty stream
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        // Malformed JSON
        InputStream malformed = new ByteArrayInputStream("{ type: object".getBytes(StandardCharsets.UTF_8));
        // YAML content (valid YAML but method forces JSON)
        InputStream yaml = new ByteArrayInputStream("type: object".getBytes(StandardCharsets.UTF_8));
        // Stream that throws IOException on read
        InputStream ioError = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced");
            }
        };
        // Stream with trailing garbage
        InputStream trailingGarbage = new ByteArrayInputStream("{\"type\":\"object\"}xyz".getBytes(StandardCharsets.UTF_8));
        // UTF‑16LE encoded JSON (Jackson expects UTF‑8)
        byte[] utf16leBom = new byte[] { (byte) 0xFF, (byte) 0xFE };
        byte[] utf16leJson = "{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_16LE);
        byte[] utf16leCombined = new byte[utf16leBom.length + utf16leJson.length];
        System.arraycopy(utf16leBom, 0, utf16leCombined, 0, utf16leBom.length);
        System.arraycopy(utf16leJson, 0, utf16leCombined, utf16leBom.length, utf16leJson.length);
        InputStream utf16le = new ByteArrayInputStream(utf16leCombined);
        // Closed stream
        ByteArrayInputStream closed = new ByteArrayInputStream(MINIMAL_SCHEMA.getBytes(StandardCharsets.UTF_8));
        try {
            closed.close();
        } catch (IOException ignored) {
        }
        // Truncated JSON (missing closing brace)
        InputStream truncated = new ByteArrayInputStream("{\"type\":\"object\"".getBytes(StandardCharsets.UTF_8));
        return Stream.of(Arguments.of("Empty InputStream → IOException/JsonSchemaException", empty, IOException.class), Arguments.of("Malformed JSON → JsonParseException", malformed, JsonParseException.class), Arguments.of("YAML content treated as JSON → JsonParseException", yaml, JsonParseException.class), Arguments.of("InputStream that throws IOException on read → IOException", ioError, IOException.class), Arguments.of("Trailing non‑JSON garbage → JsonParseException", trailingGarbage, JsonParseException.class), Arguments.of("UTF‑16LE encoded content → JsonParseException", utf16le, JsonParseException.class), Arguments.of("Closed InputStream → IOException", closed, IOException.class), Arguments.of("Truncated JSON → JsonParseException", truncated, JsonParseException.class));
    }

    /**
     * Utility method to create an {@link InputStream} that contains a UTF‑8
     * BOM followed by the supplied JSON text.
     */
    private static InputStream streamWithBom(String json) {
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + jsonBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(jsonBytes, 0, combined, bom.length, jsonBytes.length);
        return new ByteArrayInputStream(combined);
    }

    /**
     * Verifies that a well‑formed JSON schema supplied via an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance.
     */
    @Test
    void getSchema_fromInputStream_returnsJsonSchema() {
        // given
        InputStream schemaStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // when
        JsonSchema schema = factory.getSchema(schemaStream, config);
        // then
        assertNotNull(schema, "The factory should return a non‑null JsonSchema for a valid stream");
        // A minimal sanity check – the schema must have a non‑null root node.
        // JsonSchema exposes the parsed node via the {@code getSchemaNode()} method.
        assertNotNull(schema.getSchemaNode(), "The underlying JsonNode of the schema must not be null");
    }

    /**
     * Verifies that passing {@code null} as the {@link InputStream} argument
     * results in a {@link NullPointerException}. The factory is expected to
     * fail fast because the stream is required to read the schema definition.
     */
    @Test
    void getSchema_nullInputStream_throwsNullPointerException() {
        // given
        InputStream nullStream = null;
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // when / then
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullStream, config), "Calling getSchema with a null InputStream should throw NullPointerException");
    }

    @Test
    @DisplayName("Null config falls back to factory default configuration")
    void getSchema_nullConfig_usesDefaultConfig() {
        InputStream schemaStream = new ByteArrayInputStream(MINIMAL_SCHEMA.getBytes(StandardCharsets.UTF_8));
        // Pass null for the config argument
        JsonSchema schema = factory.getSchema(schemaStream, null);
        assertNotNull(schema, "Schema should still be created when config is null");
        // The factory creates a default config internally; we simply verify that the schema works.
        JsonNode instance = new ObjectMapper().readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> messages = schema.validate(instance);
        assertTrue(messages.isEmpty(), "Valid instance must produce no validation errors");
    }

    @Test
    @DisplayName("UTF‑8 BOM is ignored and schema parses successfully")
    void getSchema_utf8Bom_isAccepted() {
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] json = MINIMAL_SCHEMA.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + json.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(json, 0, combined, bom.length, json.length);
        InputStream stream = new ByteArrayInputStream(combined);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema should be created even when a BOM is present");
    }

    @Test
    @DisplayName("Leading/trailing whitespace does not affect parsing")
    void getSchema_whitespace_isAccepted() {
        String padded = "\n  " + MINIMAL_SCHEMA + "  \n\n";
        InputStream stream = new ByteArrayInputStream(padded.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema should be parsed successfully despite surrounding whitespace");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    @DisplayName("Invalid inputs should result in parsing/IO exceptions")
    void getSchema_invalidInputs_throwException(String description, InputStream stream, Class<? extends Exception> expectedException) {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(expectedException, () -> factory.getSchema(stream, config), description);
    }

    @Test
    @DisplayName("Non‑object root JSON should cause JsonSchemaException")
    void getSchema_nonObjectRoot_throwsJsonSchemaException() {
        InputStream stream = new ByteArrayInputStream("true".getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, config), "Root JSON that is not an object must cause JsonSchemaException");
    }

    @Test
    @DisplayName("Unknown $schema meta‑schema URI results in JsonSchemaException")
    void getSchema_unknownMetaSchema_throwsJsonSchemaException() {
        String unknownMeta = """
            {
              "$schema": "http://example.com/unknown",
              "type": "object"
            }
            """;
        InputStream stream = new ByteArrayInputStream(unknownMeta.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, config), "An unknown meta‑schema URI should cause JsonSchemaException");
    }

    @Test
    @DisplayName("Unresolvable $ref leads to JsonSchemaException")
    void getSchema_unresolvableRef_throwsJsonSchemaException() {
        String refSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "http://nonexistent.example/schema.json"
            }
            """;
        InputStream stream = new ByteArrayInputStream(refSchema.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, config), "Unresolvable $ref must result in JsonSchemaException");
    }

    @Test
    @DisplayName("Custom SchemaValidatorsConfig is propagated to the created JsonSchema")
    void getSchema_customConfig_isPropagated() throws Exception {
        // Build a config that disables type loosening (strict type checking)
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().failFast(true).typeLoose(false).build();
        InputStream stream = new ByteArrayInputStream(INTEGER_PROPERTY_SCHEMA.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema must be created with custom configuration");
        // Validate an instance that violates the strict integer rule
        JsonNode badInstance = new ObjectMapper().readTree("{\"age\":\"twenty\"}");
        Set<ValidationMessage> messages = schema.validate(badInstance);
        // Because typeLoose is false, the string should be considered invalid.
        assertFalse(messages.isEmpty(), "With typeLoose=false the validation must report a type error for a string value");
    }
}
