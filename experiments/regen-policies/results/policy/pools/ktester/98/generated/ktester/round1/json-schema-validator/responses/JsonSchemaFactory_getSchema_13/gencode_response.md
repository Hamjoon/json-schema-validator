```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, InputFormat, SchemaValidatorsConfig)}.
 *
 * The test class follows the provided framework – the {@code @BeforeEach} method creates a
 * default {@link JsonSchemaFactory} instance using the 2020‑12 meta‑schema.
 *
 * Each test case from the JSON specification is represented by an individual test method
 * (or a parameterized test where it makes sense).  The assertions focus on the contract:
 * <ul>
 *   <li>When parsing succeeds a non‑null {@link JsonSchema} must be returned.</li>
 *   <li>When an {@link IOException} (including parsing errors) occurs the method must
 *       wrap it in a {@link JsonSchemaException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_13_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default 2020‑12 meta‑schema.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Happy‑path: a minimal valid JSON schema is read from the stream and a {@link JsonSchema}
     * instance is returned.
     */
    @Test
    @DisplayName("Valid JSON schema → JsonSchema instance")
    void getSchema_successfulParsing_returnsJsonSchema() {
        // GIVEN
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, InputFormat.JSON, config);

        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        assertTrue(result instanceof JsonSchema, "Result should be an instance of JsonSchema");
    }

    /**
     * When the underlying {@link InputStream} throws an {@link IOException},
     * the method must wrap it in a {@link JsonSchemaException}.
     */
    @Test
    @DisplayName("IOException while reading stream → JsonSchemaException")
    void getSchema_ioException_throwsJsonSchemaException() throws IOException {
        // GIVEN
        InputStream failingStream = mock(InputStream.class);
        // Simulate a read that always throws IOException
        when(failingStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated read failure"));
        when(failingStream.read()).thenThrow(new IOException("simulated read failure"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(failingStream, InputFormat.JSON, config));
        assertTrue(ex.getCause() instanceof IOException,
                "Cause of JsonSchemaException should be the original IOException");
    }

    /**
     * Null {@link InputStream} should result in a {@link JsonSchemaException}
     * (the underlying {@code readTree} will hit a NPE).
     */
    @Test
    @DisplayName("Null InputStream → JsonSchemaException")
    void getSchema_nullInputStream_throwsJsonSchemaException() {
        // GIVEN
        InputStream nullStream = null;
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(nullStream, InputFormat.JSON, config));
        assertTrue(ex.getCause() instanceof NullPointerException,
                "Cause should be NullPointerException when stream is null");
    }

    /**
     * Null {@link InputFormat} is not acceptable – the mapper selection fails and an
     * exception is thrown.
     */
    @Test
    @DisplayName("Null InputFormat → JsonSchemaException")
    void getSchema_nullInputFormat_throwsJsonSchemaException() {
        // GIVEN
        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaStream, null, config));
        assertTrue(ex.getCause() instanceof NullPointerException,
                "Cause should be NullPointerException when input format is null");
    }

    /**
     * The {@code config} argument is optional – passing {@code null} must not break the
     * method; a valid {@link JsonSchema} should still be produced.
     */
    @Test
    @DisplayName("Null Config → JsonSchema instance")
    void getSchema_nullConfig_returnsJsonSchema() {
        // GIVEN
        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());
        SchemaValidatorsConfig nullConfig = null;

        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, InputFormat.JSON, nullConfig);

        // THEN
        assertNotNull(result, "Result should be non‑null even when config is null");
    }

    /**
     * Verify that YAML input is correctly parsed when {@link InputFormat#YAML} is supplied.
     */
    @Test
    @DisplayName("YAML InputFormat → JsonSchema instance")
    void getSchema_yamlInput_returnsJsonSchema() {
        // GIVEN
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN
        JsonSchema result = factory.getSchema(yamlStream, InputFormat.YAML, config);

        // THEN
        assertNotNull(result, "YAML schema should be parsed into a JsonSchema instance");
    }

    /**
     * An empty document cannot be parsed into a {@link JsonNode}; the method must wrap the
     * resulting {@link IOException} (actually a {@link com.fasterxml.jackson.core.JsonProcessingException})
     * in a {@link JsonSchemaException}.
     */
    @Test
    @DisplayName("Empty schema content → JsonSchemaException")
    void getSchema_emptyContent_throwsJsonSchemaException() {
        // GIVEN
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(emptyStream, InputFormat.JSON, config));
        assertTrue(ex.getCause() instanceof IOException,
                "Cause should be an IOException (JsonProcessingException) for empty content");
    }

    /**
     * Malformed JSON must cause a parsing exception which is wrapped as {@link JsonSchemaException}.
     */
    @Test
    @DisplayName("Malformed JSON → JsonSchemaException")
    void getSchema_malformedJson_throwsJsonSchemaException() {
        // GIVEN
        String malformed = "{\"type\":\"object\""; // missing closing brace
        InputStream malformedStream = new ByteArrayInputStream(malformed.getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(malformedStream, InputFormat.JSON, config));
        assertTrue(ex.getCause() instanceof IOException,
                "Cause should be an IOException (JsonProcessingException) for malformed JSON");
    }

    /**
     * Stress test with a relatively large schema (1000 string properties). The method should
     * still succeed without throwing.
     */
    @Test
    @DisplayName("Large valid schema → JsonSchema instance")
    void getSchema_largeValidSchema_returnsJsonSchema() {
        // GIVEN
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < 1000; i++) {
            sb.append("\"p").append(i).append("\":{\"type\":\"string\"}");
            if (i < 999) sb.append(',');
        }
        sb.append("}}");
        InputStream largeStream = new ByteArrayInputStream(sb.toString().getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN
        JsonSchema result = factory.getSchema(largeStream, InputFormat.JSON, config);

        // THEN
        assertNotNull(result, "Large schema should be parsed without error");
    }
}
```