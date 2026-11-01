```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Factory instance used in the tests. */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for the happy‑path test. */
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
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a factory instance for the 2020‑12 draft. The static factory method
        // builds a fully‑initialised JsonSchemaFactory with all default dependencies,
        // which is sufficient for exercising getSchema(InputStream).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void getSchema_withValidInputStream_returnsJsonSchema() {
        // GIVEN a valid InputStream containing a JSON schema
        InputStream schemaStream = new ByteArrayInputStream(
                SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));

        // WHEN the factory parses the stream
        JsonSchema jsonSchema = factory.getSchema(schemaStream);

        // THEN a non‑null JsonSchema is returned and its meta‑schema matches the draft version
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // The meta‑schema IRI for draft‑2020‑12 is defined in JsonMetaSchema; we can verify it
        JsonMetaSchema meta = jsonSchema.getMetaSchema();
        assertNotNull(meta, "Meta schema should not be null");
        assertEquals("https://json-schema.org/draft/2020-12/schema", meta.getUri(),
                "Meta schema IRI should correspond to the 2020‑12 draft");
    }

    @Test
    void getSchema_withNullInputStream_throwsNullPointerException() {
        // GIVEN a null InputStream
        InputStream nullStream = null;

        // WHEN getSchema is invoked
        // THEN a NullPointerException (or IllegalArgumentException) is expected.
        // The actual implementation throws NPE when trying to read from the stream,
        // so we assert that any NPE is thrown.
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullStream));
    }
}
```