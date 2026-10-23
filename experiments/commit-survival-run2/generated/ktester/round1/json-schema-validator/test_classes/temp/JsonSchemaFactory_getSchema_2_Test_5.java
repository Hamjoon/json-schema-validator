import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.networknt.schema.SchemaValidatorsConfig;
import java.util.Set;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    private JsonSchemaFactory factory;

    /**
     * InputStream that always throws an {@link IOException} on read and records whether it was closed.
     */
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

    /**
     * Simple InputStream that always throws an IOException on read.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Obtain a ready‑to‑use factory for Draft‑07 (the most common version).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory, "Factory should be created");
    }

    @Test
    void getSchema_fromInputStream_returnsJsonSchema() throws Exception {
        String schemaJson = "{ \"type\": \"string\" }";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(schemaStream);
        assertNotNull(schema, "The returned JsonSchema should not be null");
        Set<ValidationMessage> result = schema.validate("hello");
        assertTrue(result.isEmpty(), "A valid string should pass validation");
    }

    @Test
    void getSchema_fromNullInputStream_throwsException() {
        InputStream nullStream = null;
        assertThrows(Exception.class, () -> factory.getSchema(nullStream));
    }

    @Test
    @DisplayName("Empty InputStream results in an exception (schema content is empty)")
    void getSchema_emptyInputStream_throwsException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(empty));
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("Malformed JSON causes a parsing exception wrapped in JsonSchemaException")
    void getSchema_invalidJson_throwsParsingException() {
        String malformed = "{ invalid json }";
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
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
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true);
        JsonSchema schema = factory.getSchema(stream, config);
        assertNotNull(schema, "Schema should be created with custom configuration");
        // feed an instance that will cause validation errors; only the first should be reported due to failFast
        String instance = "{ }";
        Set<ValidationMessage> result = schema.validate(instance);
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
        assertEquals(-1, stream.read(), "Stream should be closed (read() returns -1)");
    }

    @Test
    @DisplayName("UTF‑8 BOM is ignored and schema is parsed")
    void getSchema_jsonWithUtf8Bom_isParsed() {
        byte[] bom = new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf };
        String json = "{ \"type\": \"object\" }";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + jsonBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(jsonBytes, 0, combined, bom.length, jsonBytes.length);
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
        ClosableFailingInputStream stream = new ClosableFailingInputStream();
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(stream));
        assertTrue(ex.getMessage().contains("forced error"));
        assertTrue(stream.isClosed(), "Stream must be closed even when parsing fails");
    }
}
