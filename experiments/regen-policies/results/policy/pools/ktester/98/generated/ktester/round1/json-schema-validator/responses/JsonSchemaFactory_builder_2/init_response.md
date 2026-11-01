```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 */
class JsonSchemaFactory_builder_2_Test {

    private static final String CUSTOM_SCHEMA_ID = "https://example.com/custom-schema";
    private static final String CUSTOM_SCHEMA_JSON = "{\"type\":\"string\"}";

    private JsonSchemaFactory blueprint;
    private JsonSchemaFactory copyFactory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a blueprint factory with a custom schema loader.
        blueprint = JsonSchemaFactory.builder()
                .schemaLoaders(schemaLoaders -> schemaLoaders.schemas(
                        Collections.singletonMap(CUSTOM_SCHEMA_ID, CUSTOM_SCHEMA_JSON)))
                .build();

        // Create a copy builder from the blueprint and build the copy.
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        copyFactory = copyBuilder.build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void builderCopiesCoreComponents() {
        // Verify that the ObjectMapper instances are the same (they are package‑private getters).
        assertSame(blueprint.getJsonMapper(), copyFactory.getJsonMapper(),
                "JsonMapper should be copied by the builder");
        assertSame(blueprint.getYamlMapper(), copyFactory.getYamlMapper(),
                "YamlMapper should be copied by the builder");

        // Verify that the JsonNodeReader is copied.
        assertSame(blueprint.jsonNodeReader, copyFactory.jsonNodeReader,
                "JsonNodeReader should be copied by the builder");
    }

    @Test
    void builderCopiesMetaSchemas() {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // Default meta‑schema IRI for draft‑2020‑12 is known.
        String defaultIri = "https://json-schema.org/draft/2020-12/schema";

        JsonMetaSchema metaFromBlueprint = blueprint.getMetaSchema(defaultIri, config);
        JsonMetaSchema metaFromCopy = copyFactory.getMetaSchema(defaultIri, config);

        assertNotNull(metaFromBlueprint, "Blueprint must contain the default meta‑schema");
        assertNotNull(metaFromCopy, "Copy must contain the default meta‑schema");
        assertSame(metaFromBlueprint, metaFromCopy,
                "Meta‑schema instances should be identical after copying");
    }

    @Test
    void builderCopiesSchemaLoaders() {
        // The custom schema should be loadable from both factories.
        JsonSchema schemaFromBlueprint = blueprint.getSchema(CUSTOM_SCHEMA_ID);
        JsonSchema schemaFromCopy = copyFactory.getSchema(CUSTOM_SCHEMA_ID);

        assertNotNull(schemaFromBlueprint, "Blueprint must load the custom schema");
        assertNotNull(schemaFromCopy, "Copy must load the custom schema");

        // Validate a valid instance against the custom schema (a simple string).
        assertDoesNotThrow(() -> schemaFromBlueprint.validate("a string"));
        assertDoesNotThrow(() -> schemaFromCopy.validate("another string"));
    }
}
```