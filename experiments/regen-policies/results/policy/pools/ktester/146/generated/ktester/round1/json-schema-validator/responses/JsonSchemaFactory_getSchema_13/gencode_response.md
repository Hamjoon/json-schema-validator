```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, InputFormat, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful loading for JSON and YAML formats, with and without an explicit config.</li>
 *   <li>Error handling for {@link IOException}, {@code null} arguments and format mismatches.</li>
 *   <li>Behaviour with custom configurations (e.g. {@code failFast}).</li>
 *   <li>Edge‑cases such as empty streams, partial‑read failures and internal {@code null} JsonNode.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_13_Test {

    /** Factory instance used for the tests. */
    private JsonSchemaFactory factory;

    /** Configuration used for schema validation. */
    private SchemaValidatorsConfig config;

    /** Input format for the schema (JSON or YAML). */
    private InputFormat inputFormat;

    /** A simple JSON schema used in many tests. */
    private InputStream simpleSchemaStream;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the Draft‑2020‑12 meta‑schema – the default for the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        config = SchemaValidatorsConfig.builder().build();
        inputFormat = InputFormat.JSON;

        String schemaJson = "{\"type\":\"object\"}";
        simpleSchemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (simpleSchemaStream != null) {
            simpleSchemaStream.close();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------
     *  Successful schema loading
     * -------------------------------------------------------------- */

    private static Stream<Arguments> provideSuccessfulInputs() {
        String json = "{\"type\":\"object\"}";
        String yaml = "type: object";

        return Stream.of(
                // JSON + explicit config
                Arguments.of(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build(),
                        "JSON with explicit config"),
                // YAML + explicit config
                Arguments.of(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                        InputFormat.YAML,
                        SchemaValidatorsConfig.builder().build(),
                        "YAML with explicit config"),
                // JSON + null config (factory should fall back to defaults)
                Arguments.of(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                        InputFormat.JSON,
                        null,
                        "JSON with null config")
        );
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideSuccessfulInputs")
    void shouldReturnJsonSchemaWhenInputIsValid(InputStream schemaStream,
                                                InputFormat fmt,
                                                SchemaValidatorsConfig cfg,
                                                String description) throws IOException {
        // Act
        JsonSchema schema = factory.getSchema(schemaStream, fmt, cfg);

        // Assert
        assertNotNull(schema, "A non‑null JsonSchema should be returned");
        // The returned schema must be usable – validate an empty JSON object.
        assertDoesNotThrow(() -> schema.validate("{}"),
                "The schema should be able to validate a minimal instance without throwing");
    }

    /* --------------------------------------------------------------
     *  Error handling – IOException from the stream
     * -------------------------------------------------------------- */

    @Test
    void shouldWrapIOExceptionInJsonSchemaException(@Mock InputStream failingStream) throws IOException {
        // Simulate a read failure on any read operation.
        when(failingStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated read failure"));
        when(failingStream.read()).thenThrow(new IOException("simulated read failure"));

        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(failingStream, InputFormat.JSON, config),
                "Expected JsonSchemaException when the underlying InputStream throws IOException");

        assertTrue(thrown.getCause() instanceof IOException,
                "The cause of the JsonSchemaException should be the original IOException");
        assertEquals("simulated read failure", thrown.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  Error handling – null arguments
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowExceptionWhenInputFormatIsNull() {
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream, null, config),
                "A null InputFormat should lead to JsonSchemaException (parsing fails)");
    }

    @Test
    void shouldThrowExceptionWhenInputStreamIsNull() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, InputFormat.JSON, config),
                "A null InputStream should result in NullPointerException");
    }

    @Test
    void shouldThrowExceptionWhenStreamIsEmpty() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(empty, InputFormat.JSON, config),
                "Empty content cannot be parsed and should raise JsonSchemaException");
    }

    @Test
    void shouldThrowExceptionWhenContentDoesNotMatchFormat() {
        // JSON content but declared as YAML – parsing will fail.
        String json = "{\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream, InputFormat.YAML, config),
                "Mismatched format (JSON content as YAML) should cause JsonSchemaException");
    }

    /* --------------------------------------------------------------
     *  Custom configuration – failFast
     * -------------------------------------------------------------- */

    @Test
    void shouldHonorCustomConfiguration_failFastEnabled() {
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder()
                .failFast(true)
                .build();

        JsonSchema schema = factory.getSchema(simpleSchemaStream, InputFormat.JSON, customConfig);
        assertNotNull(schema, "Schema should be created even when failFast is enabled");
        // The failFast flag influences validation, not schema creation.
        assertDoesNotThrow(() -> schema.validate("{}"));
    }

    /* --------------------------------------------------------------
     *  Large schema handling
     * -------------------------------------------------------------- */

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

    /* --------------------------------------------------------------
     *  Partial stream IOException
     * -------------------------------------------------------------- */

    private static class PartialFailInputStream extends InputStream {
        private final byte[] data;
        private int pos = 0;
        private final int failAfter; // number of bytes after which we throw

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

    @Test
    void shouldWrapPartialIOExceptionInJsonSchemaException() {
        String json = "{\"type\":\"object\"}";
        InputStream flaky = new PartialFailInputStream(json.getBytes(StandardCharsets.UTF_8), 5);
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(flaky, InputFormat.JSON, config));
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated partial failure", thrown.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  Unsupported InputFormat (simulated via reflection)
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedInputFormat() throws Exception {
        // Create a dummy enum constant via reflection – not part of the real enum.
        // This mimics a caller passing an unknown enum value.
        InputFormat unknown = (InputFormat) Enum.valueOf((Class<Enum>) InputFormat.class, "JSON"); // fallback
        // Use reflection to set a bogus name that the factory cannot recognise.
        java.lang.reflect.Field nameField = Enum.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(unknown, "UNKNOWN");

        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(stream, unknown, config),
                "Unsupported InputFormat should result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Null JsonNode returned from readTree
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowNullPointerExceptionWhenReadTreeReturnsNull() throws Exception {
        // Spy the factory so we can stub readTree.
        JsonSchemaFactory spyFactory = spy(factory);
        doReturn(null).when(spyFactory).readTree(any(InputStream.class), any(InputFormat.class));

        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class,
                () -> spyFactory.getSchema(stream, InputFormat.JSON, config),
                "If readTree returns null, newJsonSchema will receive a null node and throw NPE");
    }

    /* --------------------------------------------------------------
     *  Invalid schema content (unsupported type) – newJsonSchema failure
     * -------------------------------------------------------------- */

    @Test
    void shouldPropagateJsonSchemaExceptionWhenSchemaIsSemanticallyInvalid() {
        // The library treats an unknown type as an error during schema creation.
        String invalidSchema = "{\"type\":\"unknown\"}";
        InputStream stream = new ByteArrayInputStream(invalidSchema.getBytes(StandardCharsets.UTF_8));

        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(stream, InputFormat.JSON, config),
                "A semantically invalid schema should cause JsonSchemaException");
        // The exact message varies by implementation; we only assert the type.
        assertNotNull(thrown.getCause(), "The JsonSchemaException should have a cause");
    }
}
```