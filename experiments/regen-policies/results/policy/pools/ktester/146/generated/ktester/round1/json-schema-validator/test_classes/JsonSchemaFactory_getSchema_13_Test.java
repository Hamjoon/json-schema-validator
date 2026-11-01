package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_13_Test {

    /**
     * Factory instance used for the tests.
     */
    private JsonSchemaFactory factory;

    /**
     * Configuration used for schema validation.
     */
    private SchemaValidatorsConfig config;

    /**
     * Input format for the schema (JSON or YAML).
     */
    private InputFormat inputFormat;

    /**
     * A simple JSON schema used in the happy‑path test.
     */
    private InputStream simpleSchemaStream;

    private static class PartialFailInputStream extends InputStream {

        private final byte[] data;

        private int pos = 0;

        // number of bytes after which we throw
        private final int failAfter;

        PartialFailInputStream(byte[] data, int failAfter) {
            this.data = data;
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (pos >= failAfter) {
                throw new IOException("simulated partial failure");
            }
            return (pos < data.length) ? (data[pos++] & 0xFF) : -1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory with the default meta‑schema for Draft‑2020‑12.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Use the default configuration (all builder defaults are fine for the test).
        config = SchemaValidatorsConfig.builder().build();
        // The method under test supports both JSON and YAML; we test JSON here.
        inputFormat = InputFormat.JSON;
        // Minimal valid JSON schema – an object type without any constraints.
        String schemaJson = "{\"type\":\"object\"}";
        simpleSchemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the stream is closed after each test to avoid resource leaks.
        if (simpleSchemaStream != null) {
            simpleSchemaStream.close();
        }
    }

    private static Stream<Arguments> provideSuccessfulInputs() {
        String json = "{\"type\":\"object\"}";
        String yaml = "type: object";
        return Stream.of(// JSON + explicit config
        Arguments.of(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), InputFormat.JSON, SchemaValidatorsConfig.builder().build(), "JSON with explicit config"), // YAML + explicit config
        Arguments.of(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), InputFormat.YAML, SchemaValidatorsConfig.builder().build(), "YAML with explicit config"), // JSON + null config (factory should fall back to defaults)
        Arguments.of(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), InputFormat.JSON, null, "JSON with null config"));
    }

    /**
     * Verify that a well‑formed JSON schema stream is parsed correctly and a
     * {@link JsonSchema} instance is returned.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputIsValid() {
        JsonSchema schema = factory.getSchema(simpleSchemaStream, inputFormat, config);
        assertNotNull(schema, "The factory should return a non‑null JsonSchema");
        // The returned schema should have the same root node as the input.
        // We can check the type of the underlying JsonNode via the schema's
        // validation context – however the public API does not expose the node
        // directly. A simple sanity check is that the schema can be used to
        // validate an empty JSON object without throwing.
        assertDoesNotThrow(() -> schema.validate("{}"));
    }

    /**
     * Simulate an {@link IOException} while reading the schema stream and verify
     * that the factory wraps it in a {@link JsonSchemaException}.
     */
    @Test
    void shouldWrapIOExceptionInJsonSchemaException(@Mock InputStream failingStream) throws IOException {
        // Configure the mock to throw an IOException on any read operation.
        when(failingStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated read failure"));
        when(failingStream.read()).thenThrow(new IOException("simulated read failure"));
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(failingStream, inputFormat, config), "Expected JsonSchemaException when the underlying InputStream throws IOException");
        assertTrue(thrown.getCause() instanceof IOException, "The cause of the JsonSchemaException should be the original IOException");
        assertEquals("simulated read failure", thrown.getCause().getMessage());
    }

    @Test
    void shouldThrowExceptionWhenInputFormatIsNull() {
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, null, config), "A null InputFormat should lead to JsonSchemaException (parsing fails)");
    }

    @Test
    void shouldThrowExceptionWhenInputStreamIsNull() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, InputFormat.JSON, config), "A null InputStream should result in NullPointerException");
    }

    @Test
    void shouldThrowExceptionWhenStreamIsEmpty() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(empty, InputFormat.JSON, config), "Empty content cannot be parsed and should raise JsonSchemaException");
    }

    @Test
    void shouldThrowExceptionWhenContentDoesNotMatchFormat() {
        // JSON content but declared as YAML – parsing will fail.
        String json = "{\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, InputFormat.YAML, config), "Mismatched format (JSON content as YAML) should cause JsonSchemaException");
    }

    @Test
    void shouldHonorCustomConfiguration_failFastEnabled() {
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder().failFast(true).build();
        JsonSchema schema = factory.getSchema(simpleSchemaStream, InputFormat.JSON, customConfig);
        assertNotNull(schema, "Schema should be created even when failFast is enabled");
        // The failFast flag influences validation, not schema creation.
        assertDoesNotThrow(() -> schema.validate("{}"));
    }

    @Test
    void shouldHandleVeryLargeSchemaWithoutRunningOutOfMemory() {
        // Build a JSON schema with many nested objects (depth 1000).
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < 500; i++) {
            sb.append("\"p").append(i).append("\":{\"type\":\"object\",\"properties\":{");
        }
        sb.append("\"leaf\":{\"type\":\"string\"}");
        for (int i = 0; i < 500; i++) {
            sb.append("}}}");
        }
        sb.append("}}");
        InputStream largeStream = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(largeStream, InputFormat.JSON, config);
        assertNotNull(schema, "Factory should return a JsonSchema even for a large payload");
        // Validate a minimal instance that satisfies the top‑level schema.
        assertDoesNotThrow(() -> schema.validate("{\"leaf\":\"value\"}"));
    }

    @Test
    void shouldWrapPartialIOExceptionInJsonSchemaException() {
        String json = "{\"type\":\"object\"}";
        InputStream flaky = new PartialFailInputStream(json.getBytes(StandardCharsets.UTF_8), 5);
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(flaky, InputFormat.JSON, config));
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated partial failure", thrown.getCause().getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedInputFormat() throws Exception {
        // Create a dummy enum constant via reflection – not part of the real enum.
        // This mimics a caller passing an unknown enum value.
        // fallback
        InputFormat unknown = (InputFormat) Enum.valueOf((Class<Enum>) InputFormat.class, "JSON");
        // Use reflection to set a bogus name that the factory cannot recognise.
        java.lang.reflect.Field nameField = Enum.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(unknown, "UNKNOWN");
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(stream, unknown, config), "Unsupported InputFormat should result in IllegalArgumentException");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenReadTreeReturnsNull() throws Exception {
        // Spy the factory so we can stub readTree.
        JsonSchemaFactory spyFactory = spy(factory);
        doReturn(null).when(spyFactory).readTree(any(InputStream.class), any(InputFormat.class));
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> spyFactory.getSchema(stream, InputFormat.JSON, config), "If readTree returns null, newJsonSchema will receive a null node and throw NPE");
    }

    @Test
    void shouldPropagateJsonSchemaExceptionWhenSchemaIsSemanticallyInvalid() {
        // The library treats an unknown type as an error during schema creation.
        String invalidSchema = "{\"type\":\"unknown\"}";
        InputStream stream = new ByteArrayInputStream(invalidSchema.getBytes(StandardCharsets.UTF_8));
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(stream, InputFormat.JSON, config), "A semantically invalid schema should cause JsonSchemaException");
        // The exact message varies by implementation; we only assert the type.
        assertNotNull(thrown.getCause(), "The JsonSchemaException should have a cause");
    }
}
