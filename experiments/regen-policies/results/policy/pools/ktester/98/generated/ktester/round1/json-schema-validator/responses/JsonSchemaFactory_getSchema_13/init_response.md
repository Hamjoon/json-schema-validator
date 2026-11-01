```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, InputFormat, SchemaValidatorsConfig)}.
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
        // Use the default 2020‑12 meta‑schema. No custom loaders are required for the
        // simple schemas used in the tests.
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
     * Verify that a valid JSON schema supplied via an {@link InputStream} is parsed
     * and a {@link JsonSchema} instance is returned.
     */
    @Test
    void getSchema_successfulParsing_returnsJsonSchema() {
        // GIVEN
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, InputFormat.JSON, config);

        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        // The concrete JsonSchema implementation provides a public method to obtain the
        // underlying JsonNode; we use it if available, otherwise we simply rely on the
        // non‑null assertion above.
        assertTrue(result instanceof JsonSchema, "Result should be an instance of JsonSchema");
    }

    /**
     * Verify that an {@link IOException} occurring while reading the input stream
     * is wrapped in a {@link JsonSchemaException}.
     */
    @Test
    void getSchema_ioException_throwsJsonSchemaException() throws IOException {
        // GIVEN
        InputStream failingStream = mock(InputStream.class);
        // Simulate a read that always throws IOException
        when(failingStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated read failure"));
        // Also make the single‑byte read throw, because ObjectMapper may call it.
        when(failingStream.read()).thenThrow(new IOException("simulated read failure"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(failingStream, InputFormat.JSON, config),
                "An IOException while reading the stream should be wrapped in JsonSchemaException");
    }
}
```