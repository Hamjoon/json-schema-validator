```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, InputFormat, SchemaValidatorsConfig)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_13_Test {

    /** Factory instance used for the tests. */
    private JsonSchemaFactory factory;

    /** Configuration used for schema validation. */
    private SchemaValidatorsConfig config;

    /** Input format for the schema (JSON or YAML). */
    private InputFormat inputFormat;

    /** A simple JSON schema used in the happy‑path test. */
    private InputStream simpleSchemaStream;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
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

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
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
        when(failingStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("simulated read failure"));
        when(failingStream.read()).thenThrow(new IOException("simulated read failure"));

        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(failingStream, inputFormat, config),
                "Expected JsonSchemaException when the underlying InputStream throws IOException");

        assertTrue(thrown.getCause() instanceof IOException,
                "The cause of the JsonSchemaException should be the original IOException");
        assertEquals("simulated read failure", thrown.getCause().getMessage());
    }
}
```