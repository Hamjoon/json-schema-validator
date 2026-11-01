```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * The tests focus on the caching behaviour of {@code getMetaSchema} and on the
 * interaction with the protected {@code loadMetaSchema} method.  A {@code spy}
 * is used so that the real implementation of {@code getMetaSchema} is exercised
 * while {@code loadMetaSchema} is stubbed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    /** Factory under test – created as a Mockito spy so that we can stub the
     *  protected {@code loadMetaSchema} method. */
    private JsonSchemaFactory factorySpy;

    /** Dummy meta‑schema that will be returned by the stubbed {@code loadMetaSchema}. */
    private JsonMetaSchema dummyMetaSchema;

    /** A simple, default configuration used for all calls. */
    private SchemaValidatorsConfig defaultConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1. Build the minimal set of constructor arguments required by
        //    JsonSchemaFactory.  The constructor is private, therefore we use
        //    reflection to obtain an instance.
        // --------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectMapper yamlMapper = new ObjectMapper();
        JsonNodeReader jsonNodeReader = new JsonNodeReader(); // default ctor
        String defaultMetaSchemaIri = "http://example.com/default";

        // The factory expects a non‑empty map of meta‑schemas that already
        // contains an entry for the default IRI.
        JsonMetaSchema defaultMetaSchema = mock(JsonMetaSchema.class);
        ConcurrentMap<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaIri), defaultMetaSchema);

        SchemaLoaders.Builder schemaLoadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder schemaMappersBuilder = new SchemaMappers.Builder();
        boolean enableSchemaCache = true;
        JsonMetaSchemaFactory metaSchemaFactory = new JsonMetaSchemaFactory();

        // Use reflection to call the private constructor.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class,
                        ObjectMapper.class,
                        JsonNodeReader.class,
                        String.class,
                        SchemaLoaders.Builder.class,
                        SchemaMappers.Builder.class,
                        ConcurrentMap.class,
                        boolean.class,
                        JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        JsonSchemaFactory realFactory = ctor.newInstance(
                jsonMapper,
                yamlMapper,
                jsonNodeReader,
                defaultMetaSchemaIri,
                schemaLoadersBuilder,
                schemaMappersBuilder,
                metaSchemas,
                enableSchemaCache,
                metaSchemaFactory);

        // Create a spy so we can stub the protected loadMetaSchema method.
        factorySpy = Mockito.spy(realFactory);

        // --------------------------------------------------------------------
        // 2. Prepare a dummy JsonMetaSchema that will be returned by the stub.
        // --------------------------------------------------------------------
        dummyMetaSchema = mock(JsonMetaSchema.class);
        // Whenever loadMetaSchema is called, return the dummy instance.
        doReturn(dummyMetaSchema).when(factorySpy).loadMetaSchema(anyString(), any());

        // --------------------------------------------------------------------
        // 3. Build a default SchemaValidatorsConfig using the public builder.
        // --------------------------------------------------------------------
        defaultConfig = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /**
     * Verify that {@code getMetaSchema} loads a meta‑schema exactly once and
     * subsequently returns the cached instance.
     */
    @Test
    void getMetaSchema_isCachedAfterFirstLoad() {
        String customIri = "http://example.com/custom";

        // First call – should trigger loadMetaSchema.
        JsonMetaSchema first = factorySpy.getMetaSchema(customIri, defaultConfig);
        assertNotNull(first, "First call must return a non‑null meta‑schema");
        assertSame(dummyMetaSchema, first, "The returned instance must be the stubbed dummy");

        // Verify that loadMetaSchema was invoked exactly once for the given IRI.
        verify(factorySpy, times(1)).loadMetaSchema(eq(customIri), eq(defaultConfig));

        // Second call – should fetch from the internal cache, no additional load.
        JsonMetaSchema second = factorySpy.getMetaSchema(customIri, defaultConfig);
        assertSame(first, second, "Subsequent calls must return the cached instance");

        // No further interactions with loadMetaSchema.
        verifyNoMoreInteractions(factorySpy);
    }

    /**
     * Verify that different IRIs are cached independently.
     */
    @Test
    void getMetaSchema_cachesSeparateKeys() {
        String iriA = "http://example.com/schemaA";
        String iriB = "http://example.com/schemaB";

        JsonMetaSchema metaA = factorySpy.getMetaSchema(iriA, defaultConfig);
        JsonMetaSchema metaB = factorySpy.getMetaSchema(iriB, defaultConfig);

        assertNotSame(metaA, metaB, "Different IRIs must result in different cached instances");
        verify(factorySpy, times(1)).loadMetaSchema(eq(iriA), eq(defaultConfig));
        verify(factorySpy, times(1)).loadMetaSchema(eq(iriB), eq(defaultConfig));
    }

    /**
     * Verify that passing {@code null} as the IRI results in a {@link NullPointerException}
     * (the implementation delegates to {@code normalizeMetaSchemaUri} which does not accept null).
     */
    @Test
    void getMetaSchema_nullIri_throwsException() {
        assertThrows(NullPointerException.class,
                () -> factorySpy.getMetaSchema(null, defaultConfig),
                "Calling getMetaSchema with a null IRI should throw NPE");
    }
}
```