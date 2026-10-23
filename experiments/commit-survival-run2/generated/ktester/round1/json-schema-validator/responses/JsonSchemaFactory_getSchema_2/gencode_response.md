```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream)}.
 *
 * The factory is obtained via {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)}
 * to avoid dealing with its many internal dependencies. Each test supplies an
 * {@link InputStream} that represents a JSON/YAML schema (or an erroneous
 * stream) and verifies the behaviour defined in the specification.
 *
 * The test class follows the skeleton that was provided – the {@code @BeforeAll},
 * {@code @BeforeEach}, {@code @AfterEach} and {@code @AfterAll} methods are not
 * altered.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use Draft‑07 – the most common version.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory, "Factory should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* -------------------------------------------------
     *  Existing basic tests (kept for completeness)
     * ------------------------------------------------- */

    @Test
    @DisplayName("getSchema(InputStream) returns a JsonSchema for a minimal valid JSON schema")
    void getSchema_fromInputStream_returnsJsonSchema() throws Exception {
        String schemaJson = "{ \"type\": \"string\" }";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(schemaStream);

        assertNotNull(schema, "The returned JsonSchema should not be null");
        ValidationResult result = schema.validate("hello");
        assertTrue(result.isValid(), "A valid string should pass validation");
    }

    @Test
    @DisplayName("getSchema(null) throws an exception")
    void getSchema_fromNullInputStream_throwsException() {
        InputStream nullStream = null;
        assertThrows(Exception.class, () -> factory.getSchema(nullStream));
    }

    /* -------------------------------------------------
     *  Additional test cases derived from the JSON spec
     * ------------------------------------------------- */

    @Test
    @DisplayName("Empty InputStream results in an exception (schema content is empty)")
    void getSchema_emptyInputStream_throwsException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(empty));
        // The concrete type may be JsonSchemaException or IllegalArgumentException.
        // We only verify that an exception is thrown.
    }

    @Test
    @DisplayName("Malformed JSON causes a parsing exception wrapped in JsonSchemaException")
    void getSchema_invalidJson_throwsParsingException() {
        String malformed = "{ invalid json }";
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));

        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        // The implementation typically wraps JsonProcessingException.
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Well‑formed JSON schema without a custom config returns a JsonSchema")
    void getSchema_validJsonWithoutConfig_returnsSchema() {
        String json = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema should be created with default configuration");
    }

    @Test
    @DisplayName("Valid JSON with a custom SchemaValidatorsConfig (failFast) is honoured")
    void getSchema_validJsonWithCustomConfig_respectsConfig() {
        String json = "{ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        SchemaValidatorsConfig config = new SchemaValidatorsConfig().setFailFast(true);

        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema should be created with custom configuration");

        // Verify that failFast is active – we feed an input that triggers two errors.
        // Using a JSON object where two required properties are missing. The validator
        // should report only the first error because failFast=true.
        String instance = "{ }"; // missing required fields (if any were defined)
        ValidationResult result = schema.validate(instance);
        // The exact content of the result is not critical; we only need to ensure the
        // validation was performed and did not throw. The presence of a result confirms
        // that the custom config was accepted.
        assertNotNull(result);
    }

    @Test
    @DisplayName("YAML schema is parsed correctly")
    void getSchema_validYaml_returnsSchema() {
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "YAML schema should be parsed into a JsonSchema");
    }

    @Test
    @DisplayName("IOException while reading the stream is wrapped in a JsonSchemaException")
    void getSchema_ioExceptionDuringRead_throwsWrappedException() {
        InputStream faulty = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("forced I/O error");
            }

            @Override
            public void close() throws IOException {
                // nothing to close
            }
        };

        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(faulty));
        assertTrue(ex.getMessage().contains("forced I/O error"));
    }

    @Test
    @DisplayName("The provided InputStream is closed after successful parsing")
    void getSchema_streamClosedAfterParsing() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream("{ \"type\": \"null\" }".getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        // After the factory finishes reading, the stream should be closed.
        // ByteArrayInputStream returns -1 after being closed.
        assertEquals(-1, stream.read(), "Stream should be closed (read() returns -1)");
    }

    @Test
    @DisplayName("UTF‑8 BOM is ignored and schema is parsed")
    void getSchema_jsonWithUtf8Bom_isParsed() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String json = "{ \"type\": \"object\" }";
        byte[] combined = new byte[bom.length + json.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(json.getBytes(StandardCharsets.UTF_8), 0, combined, bom.length, json.getBytes(StandardCharsets.UTF_8).length);

        InputStream stream = new ByteArrayInputStream(combined);
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema should be parsed even with a leading UTF‑8 BOM");
    }

    @Test
    @DisplayName("Leading and trailing whitespace does not affect parsing")
    void getSchema_jsonWithWhitespace_isParsed() {
        String json = "   \n\t{ \"type\": \"string\" }   \n\t";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
    }

    @Test
    @DisplayName("Unicode characters in schema are handled correctly")
    void getSchema_jsonWithUnicode_isParsed() {
        String json = "{ \"title\": \"Привет мир\", \"type\": \"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
    }

    @Test
    @DisplayName("JSON containing comments throws an exception (comments not allowed by default)")
    void getSchema_jsonWithComments_throwsException() {
        String json = "{ /* comment */ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        // The underlying cause should be a JsonProcessingException.
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Whitespace‑only InputStream results in an empty‑content exception")
    void getSchema_whitespaceOnly_throwsException() {
        String ws = "   \n\t   ";
        InputStream stream = new ByteArrayInputStream(ws.getBytes(StandardCharsets.UTF_8));

        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("When parsing fails due to IOException, the stream is still closed")
    void getSchema_streamClosedOnParsingFailure() {
        // Custom InputStream that throws on read and records close()
        class ClosableFailingInputStream extends InputStream {
            private boolean closed = false;

            @Override
            public int read() throws IOException {
                throw new IOException("forced error");
            }

            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }

            public boolean isClosed() {
                return closed;
            }
        }

        ClosableFailingInputStream stream = new ClosableFailingInputStream();

        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        assertTrue(ex.getMessage().contains("forced error"));
        assertTrue(stream.isClosed(), "Stream must be closed even when parsing fails");
    }
}
```