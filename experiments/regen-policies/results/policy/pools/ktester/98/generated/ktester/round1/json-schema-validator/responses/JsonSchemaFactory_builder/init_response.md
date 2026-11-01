```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#builder()} and
 * {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 *
 * The tests focus on the public builder API – they do not access any private
 * fields of {@link JsonSchemaFactory} or its {@code Builder}.  The goal is to
 * ensure that the static {@code builder()} factory method returns a usable
 * builder instance and that a builder created from an existing factory can
 * produce a distinct factory instance.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    /** A mock meta‑schema used to satisfy the builder’s mandatory arguments. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** A map containing the single mock meta‑schema – required by the builder. */
    private Map<String, JsonMetaSchema> metaSchemaMap;

    /** Common objects used by several tests. */
    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;
    private JsonNodeReader jsonNodeReader;
    private JsonMetaSchemaFactory metaSchemaFactory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock meta‑schema.
        when(mockMetaSchema.getIri()).thenReturn("http://example.com/meta");

        // The builder requires a non‑empty map of meta‑schemas.
        metaSchemaMap = Collections.singletonMap(mockMetaSchema.getIri(), mockMetaSchema);

        // Real objects for the mapper‑related builder parameters.
        jsonMapper = new ObjectMapper();
        yamlMapper = new ObjectMapper();
        jsonNodeReader = new JsonNodeReader(); // default constructor
        metaSchemaFactory = new JsonMetaSchemaFactory(); // default constructor
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code JsonSchemaFactory.builder()} returns a non‑null builder
     * that can be used to construct a {@link JsonSchemaFactory} instance.
     */
    @Test
    void builderReturnsUsableInstance() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();

        assertNotNull(builder, "The static builder() method must not return null");

        // Populate the mandatory builder fields.
        builder.defaultMetaSchemaIri(mockMetaSchema.getIri())
               .metaSchemas(metaSchemaMap)
               .enableSchemaCache(true)
               .jsonMapper(jsonMapper)
               .yamlMapper(yamlMapper)
               .jsonNodeReader(jsonNodeReader)
               .metaSchemaFactory(metaSchemaFactory);

        // Build the factory – this should succeed without throwing.
        JsonSchemaFactory factory = builder.build();

        assertNotNull(factory, "Builder should produce a non‑null JsonSchemaFactory");
    }

    /**
     * Verify that a builder created from an existing {@link JsonSchemaFactory}
     * (via {@code builder(JsonSchemaFactory)}) can produce a distinct factory
     * instance. The test does not inspect internal state – it only checks that
     * the two factories are different objects and that the operation does not
     * fail.
     */
    @Test
    void builderWithBlueprintCreatesDistinctFactory() {
        // First, create a baseline factory using the regular builder.
        JsonSchemaFactory originalFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(mockMetaSchema.getIri())
                .metaSchemas(metaSchemaMap)
                .enableSchemaCache(true)
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .jsonNodeReader(jsonNodeReader)
                .metaSchemaFactory(metaSchemaFactory)
                .build();

        assertNotNull(originalFactory, "Original factory must be non‑null");

        // Create a new builder from the existing factory.
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(originalFactory);
        assertNotNull(copyBuilder, "builder(JsonSchemaFactory) must not return null");

        // Optionally modify a property to prove the builder is mutable.
        copyBuilder.enableSchemaCache(false);

        // Build the second factory.
        JsonSchemaFactory copiedFactory = copyBuilder.build();

        assertNotNull(copiedFactory, "Factory built from blueprint must be non‑null");
        assertNotSame(originalFactory, copiedFactory,
                "Factory created from a blueprint should be a distinct instance");
    }
}
```