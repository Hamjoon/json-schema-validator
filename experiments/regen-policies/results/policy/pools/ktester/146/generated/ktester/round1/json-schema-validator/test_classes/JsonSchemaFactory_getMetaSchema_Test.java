package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    /**
     * Factory under test – created as a Mockito spy so that we can stub the
     *  protected {@code loadMetaSchema} method.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * Dummy meta‑schema that will be returned by the stubbed {@code loadMetaSchema}.
     */
    private JsonMetaSchema dummyMetaSchema;

    /**
     * A simple, default configuration used for all calls.
     */
    private SchemaValidatorsConfig defaultConfig;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1. Build the minimal set of constructor arguments required by
        //    JsonSchemaFactory.  The constructor is private, therefore we use
        //    reflection to obtain an instance.
        // --------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectMapper yamlMapper = new ObjectMapper();
        // default ctor
        JsonNodeReader jsonNodeReader = new JsonNodeReader();
        String defaultMetaSchemaIri = "http://example.com/default";
        // contains an entry for the default IRI.
        JsonMetaSchema defaultMetaSchema = mock(JsonMetaSchema.class);
        ConcurrentMap<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaIri), defaultMetaSchema);
        SchemaLoaders.Builder schemaLoadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder schemaMappersBuilder = new SchemaMappers.Builder();
        boolean enableSchemaCache = true;
        JsonMetaSchemaFactory metaSchemaFactory = new JsonMetaSchemaFactory();
        // Use reflection to call the private constructor.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, ConcurrentMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        JsonSchemaFactory realFactory = ctor.newInstance(jsonMapper, yamlMapper, jsonNodeReader, defaultMetaSchemaIri, schemaLoadersBuilder, schemaMappersBuilder, metaSchemas, enableSchemaCache, metaSchemaFactory);
        // Create a spy so we can stub the protected loadMetaSchema method.
        factorySpy = Mockito.spy(realFactory);
        // --------------------------------------------------------------------
        // 2. Prepare a dummy JsonMetaSchema that will be returned by the stub.
        // --------------------------------------------------------------------
        dummyMetaSchema = mock(JsonMetaSchema.class);
        // Whenever loadMetaSchema is called, return the dummy instance.
        doReturn(dummyMetaSchema).when(factorySpy).loadMetaSchema(anyString(), any());
        // --------------------------------------------------------------------
        defaultConfig = SchemaValidatorsConfig.builder().build();
        // --------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectMapper yamlMapper = new ObjectMapper();
        // default ctor
        JsonNodeReader jsonNodeReader = new JsonNodeReader();
        // The factory expects a non‑empty map of meta‑schemas that already
        String defaultMetaSchemaIri = "http://example.com/default";
        // The factory expects a map that already contains the default meta‑schema.
        JsonMetaSchema defaultMetaSchema = mock(JsonMetaSchema.class);
        ConcurrentMap<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaIri), defaultMetaSchema);
        SchemaLoaders.Builder schemaLoadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder schemaMappersBuilder = new SchemaMappers.Builder();
        boolean enableSchemaCache = true;
        JsonMetaSchemaFactory metaSchemaFactory = new JsonMetaSchemaFactory();
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, ConcurrentMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        JsonSchemaFactory realFactory = ctor.newInstance(jsonMapper, yamlMapper, jsonNodeReader, defaultMetaSchemaIri, schemaLoadersBuilder, schemaMappersBuilder, metaSchemas, enableSchemaCache, metaSchemaFactory);
        // Spy the real instance – we will stub the protected loadMetaSchema method.
        factorySpy = Mockito.spy(realFactory);
        // --------------------------------------------------------------------
        dummyMetaSchema = mock(JsonMetaSchema.class);
        doReturn(dummyMetaSchema).when(factorySpy).loadMetaSchema(anyString(), any());
        // --------------------------------------------------------------------
        // 3. Build a default SchemaValidatorsConfig using the public builder.
        // --------------------------------------------------------------------
        defaultConfig = SchemaValidatorsConfig.builder().build();
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
        assertThrows(NullPointerException.class, () -> factorySpy.getMetaSchema(null, defaultConfig), "Calling getMetaSchema with a null IRI should throw NPE");
    }

    @Test
    @DisplayName("Load meta‑schema when absent – loader invoked once")
    void loadMetaSchemaWhenAbsent() {
        String iri = "http://example.com/schema";
        JsonMetaSchema result = factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig());
        assertSame(dummyMetaSchema, result);
        verify(factorySpy, times(1)).loadMetaSchema(eq(iri), any());
    }

    @Test
    @DisplayName("Return cached meta‑schema when present – loader not invoked again")
    void returnCachedWhenPresent() {
        String iri = "http://example.com/schema";
        // first call – loads
        factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig());
        // second call – should be cached
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig());
        assertSame(dummyMetaSchema, second);
        // only one invocation in total
        verify(factorySpy, times(1)).loadMetaSchema(eq(iri), any());
    }

    @Test
    @DisplayName("Normalization leads to same cache key (trailing slash)")
    void normalizationTrailingSlash() {
        String iriWithSlash = "http://example.com/schema/";
        String iriWithoutSlash = "http://example.com/schema";
        JsonMetaSchema first = factorySpy.getMetaSchema(iriWithSlash, defaultConfig);
        assertSame(dummyMetaSchema, first);
        verify(factorySpy, times(1)).loadMetaSchema(eq(iriWithSlash), eq(defaultConfig));
        JsonMetaSchema second = factorySpy.getMetaSchema(iriWithoutSlash, defaultConfig);
        assertSame(first, second);
        // loader must not be called a second time
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    @Test
    @DisplayName("Null config is accepted and forwarded")
    void nullConfigAccepted() {
        String iri = "http://example.com/another";
        JsonMetaSchema result = factorySpy.getMetaSchema(iri, null);
        assertSame(dummyMetaSchema, result);
        verify(factorySpy, times(1)).loadMetaSchema(eq(iri), isNull());
    }

    @Test
    @DisplayName("Config variance does not affect caching")
    void configVarianceDoesNotAffectCache() {
        String iri = "http://example.com/schema";
        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        // different instance
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig();
        JsonMetaSchema first = factorySpy.getMetaSchema(iri, cfg1);
        assertSame(dummyMetaSchema, first);
        verify(factorySpy, times(1)).loadMetaSchema(eq(iri), eq(cfg1));
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, cfg2);
        assertSame(first, second);
        // No additional load
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    @Test
    @DisplayName("Case‑insensitive IRI normalization")
    void caseInsensitiveNormalization() {
        String upperCaseIri = "HTTP://EXAMPLE.COM/SCHEMA";
        String lowerCaseIri = "http://example.com/schema";
        JsonMetaSchema first = factorySpy.getMetaSchema(upperCaseIri, null);
        assertSame(dummyMetaSchema, first);
        verify(factorySpy, times(1)).loadMetaSchema(eq(upperCaseIri), isNull());
        JsonMetaSchema second = factorySpy.getMetaSchema(lowerCaseIri, null);
        assertSame(first, second);
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    @Test
    @DisplayName("Exception propagation from loadMetaSchema")
    void exceptionPropagationFromLoader() {
        String badIri = "http://example.com/bad";
        RuntimeException loaderException = new RuntimeException("loader failure");
        // make the spy throw when loadMetaSchema is called with the bad IRI
        doThrow(loaderException).when(factorySpy).loadMetaSchema(eq(badIri), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> factorySpy.getMetaSchema(badIri, new SchemaValidatorsConfig()));
        assertSame(loaderException, thrown);
        // Verify that the map does not contain an entry for the bad IRI
        ConcurrentMap<String, JsonMetaSchema> map = getMetaSchemasMap(factorySpy);
        assertFalse(map.containsKey(JsonSchemaFactory.normalizeMetaSchemaUri(badIri)));
    }

    @Test
    @DisplayName("Concurrent loading results in single loader invocation")
    void concurrentLoadingSingleInvocation() throws Exception {
        String iri = "http://example.com/concurrent";
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        // make loader sleep a little to increase chance of overlap
        doAnswer(invocation -> {
            // wait until all threads are ready
            startLatch.await();
            Thread.sleep(50);
            return dummyMetaSchema;
        }).when(factorySpy).loadMetaSchema(eq(iri), any());
        List<Future<JsonMetaSchema>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(() -> {
                try {
                    // each thread signals readiness
                    startLatch.countDown();
                    JsonMetaSchema result = factorySpy.getMetaSchema(iri, defaultConfig);
                    return result;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }
        // release all waiting threads
        startLatch.await();
        doneLatch.await(5, TimeUnit.SECONDS);
        exec.shutdownNow();
        // All futures should contain the same instance
        for (Future<JsonMetaSchema> f : futures) {
            assertSame(dummyMetaSchema, f.get());
        }
        // Loader must have been called only once
        verify(factorySpy, times(1)).loadMetaSchema(eq(iri), any());
    }

    @Test
    @DisplayName("Cache size reflects distinct normalized IRIs")
    void cacheSizeReflectsDistinctKeys() throws Exception {
        // three distinct IRIs that normalise to distinct keys
        String[] iris = { "http://example.com/first", "http://example.com/second/", "HTTP://EXAMPLE.COM/THIRD" };
        for (String i : iris) {
            factorySpy.getMetaSchema(i, null);
        }
        ConcurrentMap<String, JsonMetaSchema> map = getMetaSchemasMap(factorySpy);
        // The map already contains the default meta‑schema added in setUp()
        assertEquals(4, map.size(), "Cache should contain default + three new entries");
    }

    @Test
    @DisplayName("Invalid IRI format triggers IllegalArgumentException")
    void invalidIriFormatThrows() {
        String badIri = "http://[invalid-url]";
        // The real normalizeMetaSchemaUri validates the IRI and throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getMetaSchema(badIri, new SchemaValidatorsConfig()));
    }

    @Test
    @DisplayName("normalizeMetaSchemaUri throws custom RuntimeException")
    void normalizeThrowsCustomRuntimeException() {
        String customIri = "http://example.com/custom";
        try (MockedStatic<JsonSchemaFactory> mocked = mockStatic(JsonSchemaFactory.class)) {
            mocked.when(() -> JsonSchemaFactory.normalizeMetaSchemaUri(customIri)).thenThrow(new IllegalStateException("custom failure"));
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factorySpy.getMetaSchema(customIri, new SchemaValidatorsConfig()));
            assertEquals("custom failure", ex.getMessage());
            // Ensure loader was never called
            verify(factorySpy, never()).loadMetaSchema(anyString(), any());
        }
    }

    @Test
    @DisplayName("loadMetaSchema returns null – getMetaSchema returns null")
    void loadMetaSchemaReturnsNull() {
        String iri = "http://example.com/null-schema";
        doReturn(null).when(factorySpy).loadMetaSchema(eq(iri), any());
        JsonMetaSchema result = factorySpy.getMetaSchema(iri, null);
        assertNull(result, "When loader returns null the method should return null");
        // Subsequent call should attempt to load again (no caching of null)
        factorySpy.getMetaSchema(iri, null);
        verify(factorySpy, times(2)).loadMetaSchema(eq(iri), any());
    }

    @Test
    @DisplayName("SchemaValidatorsConfig causes loadMetaSchema to throw IOException wrapped")
    void loaderThrowsIOExceptionWrapped() {
        String iri = "http://example.com/io-error";
        IOException io = new IOException("boom");
        RuntimeException wrapper = new RuntimeException(io);
        doThrow(wrapper).when(factorySpy).loadMetaSchema(eq(iri), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig()));
        assertSame(wrapper, thrown);
        assertSame(io, thrown.getCause());
    }

    @Test
    @DisplayName("Concurrent exception handling – one thread fails, others succeed")
    void concurrentExceptionHandling() throws Exception {
        String iri = "http://example.com/concurrent-exception";
        AtomicBoolean firstCall = new AtomicBoolean(true);
        // First invocation throws, later invocations succeed
        doAnswer(invocation -> {
            if (firstCall.getAndSet(false)) {
                throw new RuntimeException("first failure");
            }
            return dummyMetaSchema;
        }).when(factorySpy).loadMetaSchema(eq(iri), any());
        ExecutorService exec = Executors.newFixedThreadPool(3);
        Callable<JsonMetaSchema> task = () -> {
            try {
                return factorySpy.getMetaSchema(iri, null);
            } catch (RuntimeException e) {
                // propagate for assertion
                throw e;
            }
        };
        // may fail
        Future<JsonMetaSchema> f1 = exec.submit(task);
        // may succeed
        Future<JsonMetaSchema> f2 = exec.submit(task);
        // may succeed
        Future<JsonMetaSchema> f3 = exec.submit(task);
        // At least one future should complete exceptionally with the first failure
        int failures = 0;
        for (Future<JsonMetaSchema> f : List.of(f1, f2, f3)) {
            try {
                JsonMetaSchema res = f.get();
                assertSame(dummyMetaSchema, res);
            } catch (ExecutionException ee) {
                assertTrue(ee.getCause() instanceof RuntimeException);
                assertEquals("first failure", ee.getCause().getMessage());
                failures++;
            }
        }
        assertEquals(1, failures, "Exactly one thread should see the injected exception");
        // After the failure the map must contain the successful entry (or be empty if all failed)
        ConcurrentMap<String, JsonMetaSchema> map = getMetaSchemasMap(factorySpy);
        String key = JsonSchemaFactory.normalizeMetaSchemaUri(iri);
        assertTrue(map.containsKey(key));
        assertSame(dummyMetaSchema, map.get(key));
        exec.shutdownNow();
    }

    // ------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, JsonMetaSchema> getMetaSchemasMap(JsonSchemaFactory factory) {
        try {
            Field f = JsonSchemaFactory.class.getDeclaredField("metaSchemas");
            f.setAccessible(true);
            return (ConcurrentMap<String, JsonMetaSchema>) f.get(factory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
