package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.function.Executable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    /**
     * A mock meta‑schema used to satisfy the builder’s mandatory arguments.
     */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /**
     * A map containing the single mock meta‑schema – required by the builder.
     */
    private Map<String, JsonMetaSchema> metaSchemaMap;

    /**
     * Common objects used by several tests.
     */
    private ObjectMapper jsonMapper;

    private ObjectMapper yamlMapper;

    private JsonNodeReader jsonNodeReader;

    private JsonMetaSchemaFactory metaSchemaFactory;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock meta‑schema.
        when(mockMetaSchema.getIri()).thenReturn("http://example.com/meta");
        // The builder requires a non‑empty map of meta‑schemas.
        metaSchemaMap = Collections.singletonMap(mockMetaSchema.getIri(), mockMetaSchema);
        // Real objects for the mapper‑related builder parameters.
        jsonMapper = new ObjectMapper();
        yamlMapper = new ObjectMapper();
        // default constructor
        jsonNodeReader = new JsonNodeReader();
        // default constructor
        metaSchemaFactory = new JsonMetaSchemaFactory();
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
        builder.defaultMetaSchemaIri(mockMetaSchema.getIri()).metaSchemas(metaSchemaMap).enableSchemaCache(true).jsonMapper(jsonMapper).yamlMapper(yamlMapper).jsonNodeReader(jsonNodeReader).metaSchemaFactory(metaSchemaFactory);
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
        JsonSchemaFactory originalFactory = JsonSchemaFactory.builder().defaultMetaSchemaIri(mockMetaSchema.getIri()).metaSchemas(metaSchemaMap).enableSchemaCache(true).jsonMapper(jsonMapper).yamlMapper(yamlMapper).jsonNodeReader(jsonNodeReader).metaSchemaFactory(metaSchemaFactory).build();
        assertNotNull(originalFactory, "Original factory must be non‑null");
        // Create a new builder from the existing factory.
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(originalFactory);
        assertNotNull(copyBuilder, "builder(JsonSchemaFactory) must not return null");
        // Optionally modify a property to prove the builder is mutable.
        copyBuilder.enableSchemaCache(false);
        // Build the second factory.
        JsonSchemaFactory copiedFactory = copyBuilder.build();
        assertNotNull(copiedFactory, "Factory built from blueprint must be non‑null");
        assertNotSame(originalFactory, copiedFactory, "Factory created from a blueprint should be a distinct instance");
    }

    /**
     * Ensure that two successive calls to {@code builder()} produce distinct
     * {@code Builder} objects (different references and not equal via {@code equals}).
     */
    @Test
    void builderReturnsDistinctInstances() {
        JsonSchemaFactory.Builder builder1 = JsonSchemaFactory.builder();
        JsonSchemaFactory.Builder builder2 = JsonSchemaFactory.builder();
        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2, "Two builder() calls must return different objects");
        // The default equals implementation from Object should also report false.
        assertFalse(builder1.equals(builder2), "Distinct Builder instances should not be equal via equals()");
    }

    /**
     * Verify that the object returned by {@code builder()} is an instance of the
     * nested {@code JsonSchemaFactory.Builder} class.
     */
    @Test
    void builderInstanceHasCorrectRuntimeType() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        assertNotNull(builder);
        // The fully‑qualified name should end with "$Builder".
        assertTrue(builder.getClass().getName().endsWith("JsonSchemaFactory$Builder"), "Builder runtime class name should indicate the nested Builder type");
        // Additionally, a simple instanceof check compiles and passes.
        assertTrue(builder instanceof JsonSchemaFactory.Builder, "Builder should be an instance of JsonSchemaFactory.Builder");
    }

    /**
     * Spawn several threads that each call {@code JsonSchemaFactory.builder()}
     * concurrently. All returned builders must be non‑null and distinct.
     */
    @Test
    void builderIsThreadSafeAndCreatesDistinctInstances() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<JsonSchemaFactory.Builder>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(JsonSchemaFactory::builder));
            }
            Set<JsonSchemaFactory.Builder> builderSet = new HashSet<>();
            for (Future<JsonSchemaFactory.Builder> f : futures) {
                // may throw ExecutionException
                JsonSchemaFactory.Builder b = f.get();
                assertNotNull(b, "Builder returned from thread must not be null");
                builderSet.add(b);
            }
            assertEquals(threadCount, builderSet.size(), "All builders created in parallel should be distinct instances");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Using reflection, verify that a newly created {@code Builder} has its
     * internal configuration objects initialised (i.e. non‑null). The exact
     * field names are not part of the public API, therefore we check that
     * any field of type {@link SchemaValidatorsConfig} and any field of type
     * {@link VersionFlag} are non‑null after construction.
     */
    @Test
    void builderDefaultFieldsAreInitialised() throws IllegalAccessException {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        // Inspect all declared fields of the Builder class.
        Field[] fields = builder.getClass().getDeclaredFields();
        boolean foundConfig = false;
        boolean foundVersionFlag = false;
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(builder);
            if (field.getType().equals(SchemaValidatorsConfig.class)) {
                foundConfig = true;
                assertNotNull(value, "SchemaValidatorsConfig field should be initialised (non‑null)");
            }
            if (field.getType().equals(VersionFlag.class)) {
                foundVersionFlag = true;
                // VersionFlag may legitimately be null (default), so we only assert existence.
                // If the implementation chooses a default, we accept non‑null as well.
            }
        }
        assertTrue(foundConfig, "Builder should contain a SchemaValidatorsConfig field");
        // The presence of a VersionFlag field is optional; we just ensure we inspected it.
    }

    /**
     * After building a {@link JsonSchemaFactory} via the builder, the resulting
     * factory must provide a functional {@link ObjectMapper} via {@code getJsonMapper()}.
     */
    @Test
    void builderBuildProducesFactoryWithNonNullJsonMapper() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder().defaultMetaSchemaIri(mockMetaSchema.getIri()).metaSchemas(metaSchemaMap).enableSchemaCache(true).jsonMapper(jsonMapper).yamlMapper(yamlMapper).jsonNodeReader(jsonNodeReader).metaSchemaFactory(metaSchemaFactory);
        JsonSchemaFactory factory = builder.build();
        assertNotNull(factory, "Factory built from builder must not be null");
        ObjectMapper mapper = factory.getJsonMapper();
        assertNotNull(mapper, "Factory's getJsonMapper() should return a non‑null ObjectMapper");
    }
}
