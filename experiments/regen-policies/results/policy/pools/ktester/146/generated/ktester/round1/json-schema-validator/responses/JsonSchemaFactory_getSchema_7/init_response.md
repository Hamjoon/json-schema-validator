```java
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

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * The tests focus on the public contract of the method:
 *   – a valid {@link JsonSchema} is returned for a well‑formed JSON schema stream,
 *   – the method throws a {@link NullPointerException} when the {@code InputStream}
 *     argument is {@code null},
 *   – the supplied {@link SchemaValidatorsConfig} is accepted (the default config
 *     is used in the happy‑path test).
 *
 * The factory is created via the public {@code getInstance(...)} API; no private
 * members or reflection are used.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /** Factory shared by all tests. */
    private static JsonSchemaFactory factory;

    /** Simple JSON schema used in the happy‑path test. */
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

    @BeforeAll
    static void setupBeforeAll() {
        // Use the latest draft version; the builder is not needed for this simple test.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static factory reference.
        factory = null;
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
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullStream, config),
                "Calling getSchema with a null InputStream should throw NullPointerException");
    }
}
```