```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Factory instance shared by all tests. */
    private static JsonSchemaFactory factory;

    /** A very small, valid JSON‑Schema used for the tests. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it builds the internal constructor
        // with all required collaborators (mapper factories, loaders, etc.).
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> {
                    // No customisation needed for these tests.
                });
        assertNotNull(factory, "Factory should have been created");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        factory = null;
    }

    @Test
    void getSchema_fromValidInputStream_returnsJsonSchema() {
        // GIVEN – an InputStream that contains a valid JSON‑Schema document
        InputStream schemaStream = new ByteArrayInputStream(
                SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));

        // WHEN – the factory creates a JsonSchema from the stream
        JsonSchema jsonSchema = factory.getSchema(schemaStream);

        // THEN – a non‑null JsonSchema instance is returned
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // The returned object must be of the expected type
        assertTrue(jsonSchema instanceof JsonSchema,
                "Returned object should be an instance of JsonSchema");
    }

    @Test
    void getSchema_fromNullInputStream_throwsNullPointerException() {
        // GIVEN – a null InputStream
        InputStream nullStream = null;

        // WHEN / THEN – the method should reject the null argument
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullStream),
                "Calling getSchema(null) must throw NullPointerException");
    }
}
```