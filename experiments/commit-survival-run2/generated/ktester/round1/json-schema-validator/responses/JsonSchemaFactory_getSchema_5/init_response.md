```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /** A factory instance that can be reused for all tests. */
    private static JsonSchemaFactory factory;

    /** Temporary file that holds a minimal JSON schema used by the tests. */
    private static Path tempSchemaFile;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a very small JSON schema: {"type":"object"}
        String schemaContent = "{\"type\":\"object\"}";
        tempSchemaFile = Files.createTempFile("minimal-schema", ".json");
        Files.writeString(tempSchemaFile, schemaContent);

        // Obtain a factory instance using the public static factory method.
        // V4 is the most common version flag; adjust if needed.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory, "Factory should have been created");
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Clean up the temporary file.
        if (tempSchemaFile != null) {
            Files.deleteIfExists(tempSchemaFile);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed at the moment.
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(URI)} correctly loads a
     * schema from a valid {@link URI} and returns a non‑null {@link JsonSchema}
     * whose root node matches the supplied JSON.
     */
    @Test
    void getSchema_withValidUri_returnsJsonSchema() {
        // GIVEN
        URI schemaUri = tempSchemaFile.toUri();

        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schemaUri);

        // THEN
        assertNotNull(jsonSchema, "Returned JsonSchema should not be null");
        JsonNode rootNode = jsonSchema.getSchemaNode();
        assertNotNull(rootNode, "Root node of the schema should not be null");
        assertEquals("object", rootNode.get("type").asText(),
                "The 'type' property of the loaded schema should be 'object'");
    }

    /**
     * Verifies that passing {@code null} as the URI results in an
     * {@link IllegalArgumentException}. The actual implementation of the method
     * performs a null‑check, so the test ensures the contract is honoured.
     */
    @Test
    void getSchema_withNullUri_throwsIllegalArgumentException() {
        // GIVEN
        URI nullUri = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(nullUri),
                "Calling getSchema(null) should throw IllegalArgumentException");
    }
}
```