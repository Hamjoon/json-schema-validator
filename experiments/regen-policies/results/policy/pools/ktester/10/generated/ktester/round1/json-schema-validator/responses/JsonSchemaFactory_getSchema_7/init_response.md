```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful parsing of a valid JSON schema.</li>
 *   <li>Proper handling of malformed JSON – the method must wrap the {@link IOException}
 *       in a {@link JsonSchemaException}.</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /** Factory instance used by all tests. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema (the exact version is irrelevant for these tests).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory must be created");
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
        // Nothing to clean up globally.
        factory = null;
    }

    /**
     * Verify that a well‑formed JSON schema supplied via an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance.
     */
    @Test
    void testGetSchemaWithValidInput() throws IOException {
        // A minimal, valid JSON schema (object with no properties)
        String jsonSchema = "{\"type\":\"object\"}";
        try (InputStream schemaStream = new ByteArrayInputStream(jsonSchema.getBytes())) {
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();

            JsonSchema schema = factory.getSchema(schemaStream, config);

            assertNotNull(schema, "Returned JsonSchema must not be null");
            // The concrete class is package‑private, but we can assert the type via the interface.
            assertTrue(schema instanceof JsonSchema, "Result should be an instance of JsonSchema");
        }
    }

    /**
     * Verify that malformed JSON causes a {@link JsonSchemaException}
     * to be thrown. The underlying {@link ObjectMapper} throws an {@link IOException},
     * which the method should wrap.
     */
    @Test
    void testGetSchemaWithInvalidInputThrowsException() {
        // Deliberately malformed JSON
        String malformed = "this is not json";
        InputStream schemaStream = new ByteArrayInputStream(malformed.getBytes());
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        JsonSchemaException thrown = assertThrows(
                JsonSchemaException.class,
                () -> factory.getSchema(schemaStream, config),
                "Expected JsonSchemaException for malformed JSON"
        );

        // The cause should be an IOException (often a JsonParseException)
        assertTrue(thrown.getCause() instanceof IOException,
                "Cause of JsonSchemaException should be an IOException");
    }
}
```