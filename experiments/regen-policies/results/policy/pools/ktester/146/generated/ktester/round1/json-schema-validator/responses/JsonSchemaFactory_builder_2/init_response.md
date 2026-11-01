```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    private JsonSchemaFactory blueprint;
    private JsonMetaSchema metaSchema;
    private SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // use a known meta‑schema (draft‑2020‑12) as the default
        metaSchema = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V202012);
        config = SchemaValidatorsConfig.builder().build();

        // build a factory with custom meta‑schema, default IRI and a custom schema loader
        blueprint = JsonSchemaFactory.builder()
                .metaSchema(metaSchema)                                 // register the meta‑schema
                .defaultMetaSchemaIri(metaSchema.getIri())              // set it as default
                .schemaLoaders(sl -> sl.schemas(
                        Map.of("urn:test", "{\"type\":\"string\"}")))   // custom schema loader entry
                .build();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    /**
     * Verify that the static {@code builder(JsonSchemaFactory)} method copies
     * meta‑schemas and the default meta‑schema IRI from the blueprint.
     */
    @Test
    void builderCopiesMetaSchemasAndDefaultIri() {
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprint);
        JsonSchemaFactory copiedFactory = copiedBuilder.build();

        // The meta‑schema retrieved from the copied factory must be the same instance
        JsonMetaSchema originalMeta = blueprint.getMetaSchema(metaSchema.getIri(), config);
        JsonMetaSchema copiedMeta = copiedFactory.getMetaSchema(metaSchema.getIri(), config);
        assertSame(originalMeta, copiedMeta,
                "Meta schema should be identical after copying the builder");

        // The default meta‑schema IRI is implicitly verified by the above call;
        // if it were different, getMetaSchema would have thrown an exception.
    }

    /**
     * Verify that the schema loaders configured on the blueprint are also
     * present on a factory created from the copied builder.
     */
    @Test
    void builderCopiesSchemaLoaders() {
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprint);
        JsonSchemaFactory copiedFactory = copiedBuilder.build();

        // Both factories should be able to resolve the custom schema identified by "urn:test"
        JsonSchema originalSchema = blueprint.getSchema(URI.create("urn:test"));
        JsonSchema copiedSchema = copiedFactory.getSchema(URI.create("urn:test"));

        assertNotNull(originalSchema, "Original factory should resolve the custom schema");
        assertNotNull(copiedSchema, "Copied factory should resolve the custom schema");

        // The resolved schema nodes should be equal (both represent {"type":"string"})
        assertEquals(originalSchema.getSchemaNode().toString(),
                     copiedSchema.getSchemaNode().toString(),
                     "Schema nodes loaded by original and copied factories must be equal");
    }
}
```