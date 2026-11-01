```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchemaFactory#loadMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * The tests create temporary JSON meta‑schema files on the file system and
 * invoke the protected {@code loadMetaSchema} method (accessible because the
 * test class resides in the same package). Two scenarios are covered:
 *
 * <ul>
 *   <li>A meta‑schema using the 2019‑09 specification with a $vocabulary
 *   entry – the resulting {@link JsonMetaSchema} must contain the vocabulary.</li>
 *   <li>A meta‑schema using the draft‑07 specification without a $vocabulary –
 *   the resulting {@link JsonMetaSchema} must not contain any vocabularies.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global setup required
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the 2019‑09 version as the default for the factory – it contains
        // the built‑in meta‑schemas for both V201909 and V7.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required (temporary files are deleted on exit)
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /**
     * Verify that a meta‑schema with a $vocabulary entry (and a $schema pointing
     * to the 2019‑09 meta‑schema) is loaded correctly and that the vocabulary
     * map is populated.
     */
    @Test
    void testLoadMetaSchemaWithVocabularies() throws IOException {
        // --------------------------------------------------------------------
        // GIVEN – a temporary meta‑schema file using V201909 and a $vocabulary
        // --------------------------------------------------------------------
        Path tempFile = Files.createTempFile("custom-meta-schema", ".json");
        tempFile.toFile().deleteOnExit();

        String jsonContent = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema#\",\n" +
                "  \"$id\": \"http://example.com/custom-meta-schema\",\n" +
                "  \"$vocabulary\": {\n" +
                "    \"https://json-schema.org/vocab/applicator\": true\n" +
                "  }\n" +
                "}";

        Files.write(tempFile, jsonContent.getBytes(StandardCharsets.UTF_8));

        String schemaUri = tempFile.toUri().toString();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // -------------------------------------------------
        // WHEN – the protected loadMetaSchema method is called
        // -------------------------------------------------
        JsonMetaSchema metaSchema = factory.loadMetaSchema(schemaUri, config);

        // -------------------------------------------------
        // THEN – the resulting JsonMetaSchema contains the expected data
        // -------------------------------------------------
        assertNotNull(metaSchema, "Meta schema should not be null");
        assertEquals(schemaUri, metaSchema.getId(), "Meta schema ID should match the supplied URI");
        assertEquals(VersionFlag.V201909, metaSchema.getSpecification(),
                "Specification should be V201909 because of the $schema value");

        Map<String, Boolean> vocabularies = metaSchema.getVocabularies();
        assertNotNull(vocabularies, "Vocabularies map should be present for V201909");
        assertTrue(vocabularies.containsKey("https://json-schema.org/vocab/applicator"),
                "Vocabularies should contain the applicator entry");
        assertTrue(vocabularies.get("https://json-schema.org/vocab/applicator"),
                "Applicator vocabulary should be true");
    }

    /**
     * Verify that a meta‑schema using draft‑07 (which does not support vocabularies)
     * results in a JsonMetaSchema without a vocabularies map.
     */
    @Test
    void testLoadMetaSchemaWithoutVocabularies() throws IOException {
        // --------------------------------------------------------------------
        // GIVEN – a temporary meta‑schema file using draft‑07 and no $vocabulary
        // --------------------------------------------------------------------
        Path tempFile = Files.createTempFile("custom-meta-schema-draft7", ".json");
        tempFile.toFile().deleteOnExit();

        String jsonContent = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"$id\": \"http://example.com/custom-meta-schema-draft7\"\n" +
                "}";

        Files.write(tempFile, jsonContent.getBytes(StandardCharsets.UTF_8));

        String schemaUri = tempFile.toUri().toString();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // -------------------------------------------------
        // WHEN – the protected loadMetaSchema method is called
        // -------------------------------------------------
        JsonMetaSchema metaSchema = factory.loadMetaSchema(schemaUri, config);

        // -------------------------------------------------
        // THEN – the resulting JsonMetaSchema reflects draft‑07 without vocabularies
        // -------------------------------------------------
        assertNotNull(metaSchema, "Meta schema should not be null");
        assertEquals(schemaUri, metaSchema.getId(), "Meta schema ID should match the supplied URI");
        assertEquals(VersionFlag.V7, metaSchema.getSpecification(),
                "Specification should be V7 because of the $schema value");

        // For draft‑07 the builder does not initialise the vocabularies map.
        assertNull(metaSchema.getVocabularies(),
                "Vocabularies map should be null for specifications prior to V201909");
    }
}
```