```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * The factory is obtained via the public {@code getInstance} method, which supplies
 * all required internal dependencies (ObjectMapper, URIFactory, etc.) with sensible
 * defaults.  This avoids the need to invoke any private constructors or to mock
 * the complex dependency graph of {@code JsonSchemaFactory}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory used for all test cases. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete, publicly‑available factory instance.
        // The V201909 flag is one of the supported spec versions.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory should be created successfully");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when the configuration
     * argument is {@code null}.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsNull() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, null);

        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when a non‑null
     * {@link SchemaValidatorsConfig} is supplied.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsProvided() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, config);

        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that passing a {@code null} {@link InputStream} results in a
     * {@link JsonSchemaException} (wrapped around the underlying {@link IOException}).
     */
    @Test
    void shouldThrowJsonSchemaExceptionWhenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(nullStream, null),
                "Expected JsonSchemaException when InputStream is null");
        assertNotNull(ex.getCause(), "The cause of the exception should be non‑null");
    }
}
```