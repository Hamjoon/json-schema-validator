package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    /**
     * IRI used for the custom meta‑schema in the tests.
     */
    private static final String TEST_META_SCHEMA_IRI = "https://www.example.com/test-meta-schema";

    /**
     * Minimal meta‑schema JSON – it only needs to be syntactically valid.
     */
    private static final String TEST_META_SCHEMA_JSON = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"$id\": \"" + TEST_META_SCHEMA_IRI + "\",\n" + "  \"type\": \"object\"\n" + "}";

    /**
     * Factory under test – created once per test method.
     */
    private JsonSchemaFactory factory;

    /**
     * The factory under test – a Mockito spy so that we can verify calls to {@code loadMetaSchema}.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A reusable dummy meta‑schema returned by the stubbed {@code loadMetaSchema}.
     */
    private JsonMetaSchema dummyMetaSchema;

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchemaFactory that knows about our test meta‑schema.
         * The builder registers a custom {@link SchemaLoaders} instance that
         * returns the JSON string defined above when the IRI is requested.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap(TEST_META_SCHEMA_IRI, TEST_META_SCHEMA_JSON))));
        // Create a real factory (any version works – we use V202012) and spy on it.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));
        factorySpy = Mockito.spy(realFactory);
        // Prepare a dummy meta‑schema that will be returned for every successful load.
        dummyMetaSchema = mock(JsonMetaSchema.class);
        // By default, loadMetaSchema returns the dummy instance.
        doReturn(dummyMetaSchema).when(factorySpy).loadMetaSchema(anyString(), any());
    }

    /**
     * Normalises an IRI using the same logic the production code uses.
     * The method is {@code protected static} – we invoke it via reflection.
     */
    private static String normalize(String iri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, iri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    private static Object getField(Object target, String name) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that the same {@link JsonMetaSchema} instance is returned for
     * repeated calls with the same IRI, regardless of the {@link SchemaValidatorsConfig}
     * supplied. This proves that the internal cache works as intended.
     */
    @Test
    void getMetaSchema_isCachedAcrossCalls() {
        SchemaValidatorsConfig config1 = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig config2 = SchemaValidatorsConfig.builder().build();
        JsonMetaSchema meta1 = factory.getMetaSchema(TEST_META_SCHEMA_IRI, config1);
        assertNotNull(meta1, "First call should return a non‑null meta‑schema");
        JsonMetaSchema meta2 = factory.getMetaSchema(TEST_META_SCHEMA_IRI, config2);
        assertNotNull(meta2, "Second call should also return a non‑null meta‑schema");
        assertSame(meta1, meta2, "Meta‑schema instances must be identical (cached)");
    }

    /**
     * Stress test the thread‑safety of {@code getMetaSchema}.  Fifty threads
     * invoke the method concurrently; all must obtain the same cached instance.
     */
    @Test
    void getMetaSchema_isThreadSafe() throws InterruptedException {
        final int THREAD_COUNT = 50;
        final JsonMetaSchema[] holder = new JsonMetaSchema[1];
        final AtomicBoolean failed = new AtomicBoolean(false);
        final CountDownLatch startLatch = new CountDownLatch(1);
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
                try {
                    // wait until all threads are ready
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                JsonMetaSchema meta = factory.getMetaSchema(TEST_META_SCHEMA_IRI, cfg);
                synchronized (holder) {
                    if (holder[0] == null) {
                        holder[0] = meta;
                    } else if (holder[0] != meta) {
                        failed.set(true);
                    }
                }
            }, "MetaSchema-Thread-" + i);
            threads[i].start();
        }
        // Release all threads simultaneously
        startLatch.countDown();
        // Wait for completion
        for (Thread t : threads) {
            t.join();
        }
        assertFalse(failed.get(), "All threads must receive the same JsonMetaSchema instance");
        assertNotNull(holder[0], "Meta‑schema should have been loaded by at least one thread");
    }

    /**
     * Ensure that an unknown IRI results in an {@link IllegalArgumentException}
     * because the factory cannot locate a meta‑schema for it.
     */
    @Test
    void getMetaSchema_unknownIri_throwsException() {
        String unknownIri = "https://www.example.com/unknown-meta-schema";
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> factory.getMetaSchema(unknownIri, cfg), "Requesting an unknown meta‑schema IRI should throw IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("Meta Schema for default Meta Schema URI must be provided") || thrown.getMessage().contains("cannot be null"), "Exception message should indicate a missing meta‑schema");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheMiss_loadsAndCachesMetaSchema() {
        String iri = "http://example.com/schema/meta-schema.json";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonMetaSchema result = factorySpy.getMetaSchema(iri, cfg);
        assertNotNull(result, "Result must not be null on cache miss");
        // Verify that loadMetaSchema was invoked exactly once with the normalised key.
        String expectedKey = normalize(iri);
        verify(factorySpy, times(1)).loadMetaSchema(eq(expectedKey), eq(cfg));
        // The internal map must now contain the entry.
        ConcurrentMap<String, JsonMetaSchema> map = (ConcurrentMap<String, JsonMetaSchema>) getField(factorySpy, "metaSchemas");
        assertTrue(map.containsKey(expectedKey), "Internal cache should contain the normalised key");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheHit_reusesExistingInstance() {
        String iri = "http://example.com/schema/meta-schema.json";
        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        // different instance
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig();
        JsonMetaSchema first = factorySpy.getMetaSchema(iri, cfg1);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, cfg2);
        assertSame(first, second, "Subsequent calls must return the same cached instance");
        // loadMetaSchema must have been called only once (the first call).
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void normalisationDifferentIrisMapToSameKey() {
        String iri1 = "http://example.com/schema/meta-schema.json";
        // no .json
        String iri2 = "http://example.com/schema/meta-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonMetaSchema first = factorySpy.getMetaSchema(iri1, cfg);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri2, cfg);
        assertSame(first, second, "Both IRIs should resolve to the same cached meta‑schema");
        // loadMetaSchema must have been called only once because the normalised key is identical.
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullIri_throwsException() {
        assertThrows(NullPointerException.class, () -> factorySpy.getMetaSchema(null, null), "Passing a null IRI must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullConfig_isPropagatedToLoadMetaSchema() {
        String iri = "urn:jsonschema:draft-07";
        // The stubbed loadMetaSchema will be called; capture the config argument.
        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        factorySpy.getMetaSchema(iri, null);
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), cfgCaptor.capture());
        assertNull(cfgCaptor.getValue(), "The config argument passed to loadMetaSchema must be null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void customConfig_isForwardedOnCacheMiss() {
        String iri = "http://example.com/schema/custom-meta-schema.json";
        SchemaValidatorsConfig customCfg = new SchemaValidatorsConfig();
        // a non‑default setting
        customCfg.setTypeLoose(false);
        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        factorySpy.getMetaSchema(iri, customCfg);
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), cfgCaptor.capture());
        assertSame(customCfg, cfgCaptor.getValue(), "The exact custom config instance must be forwarded to loadMetaSchema");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheHit_ignoresSubsequentConfigChanges() {
        String iri = "http://example.com/schema/custom-meta-schema.json";
        SchemaValidatorsConfig firstCfg = new SchemaValidatorsConfig();
        firstCfg.setTypeLoose(false);
        // different (null) config
        SchemaValidatorsConfig secondCfg = null;
        JsonMetaSchema first = factorySpy.getMetaSchema(iri, firstCfg);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, secondCfg);
        assertSame(first, second, "Cached instance must be returned irrespective of later config");
        // loadMetaSchema should have been called only once (the first call).
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fragmentIdentifier_isIgnoredForCacheKey() {
        String base = "http://example.com/schema/meta-schema.json";
        String withFragment = base + "#";
        factorySpy.getMetaSchema(base, null);
        factorySpy.getMetaSchema(withFragment, null);
        // Only one load should have happened.
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(base)), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void schemeCaseInsensitivity_isHandled() {
        String lower = "http://example.com/schema/meta-schema.json";
        String upper = "HTTP://example.com/schema/meta-schema.json";
        factorySpy.getMetaSchema(lower, null);
        factorySpy.getMetaSchema(upper, null);
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(lower)), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyStringIri_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getMetaSchema("", null), "Empty IRI should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void malformedIri_throwsException() {
        String badIri = "ht!tp://[invalid]";
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getMetaSchema(badIri, null), "Malformed IRI must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void trailingSlash_isIgnored() {
        String withoutSlash = "http://example.com/schema/meta-schema.json";
        String withSlash = withoutSlash + "/";
        factorySpy.getMetaSchema(withoutSlash, null);
        factorySpy.getMetaSchema(withSlash, null);
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(withoutSlash)), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentAccess_loadInvokedOnlyOnce() throws InterruptedException {
        final String iri = "http://example.com/schema/concurrent-meta-schema.json";
        final int threads = 20;
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final JsonMetaSchema[] holder = new JsonMetaSchema[1];
        final AtomicBoolean mismatch = new AtomicBoolean(false);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                JsonMetaSchema meta = factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig());
                synchronized (holder) {
                    if (holder[0] == null) {
                        holder[0] = meta;
                    } else if (holder[0] != meta) {
                        mismatch.set(true);
                    }
                }
            }, "t-" + i).start();
        }
        // Wait until all threads are ready, then fire them all at once.
        ready.await();
        start.countDown();
        // Give a little time for threads to finish.
        Thread.sleep(500);
        assertFalse(mismatch.get(), "All threads must receive the same instance");
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), any());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void unknownIri_loadFailsWithIllegalArgumentException() {
        // For this test we let the real implementation run (no stubbing) because
        // the stub would otherwise swallow the exception.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));
        assertThrows(IllegalArgumentException.class, () -> realFactory.getMetaSchema("urn:unknown:meta-schema", new SchemaValidatorsConfig()), "Unknown IRI must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void resourceNotFound_propagatesRuntimeException() {
        // Force loadMetaSchema to throw an unchecked exception.
        doThrow(new IllegalStateException("Resource not found")).when(factorySpy).loadMetaSchema(anyString(), any());
        assertThrows(IllegalStateException.class, () -> factorySpy.getMetaSchema("http://example.com/schema/missing-meta-schema.json", new SchemaValidatorsConfig()), "Missing resource should result in an unchecked exception");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void malformedMetaSchemaJson_propagatesRuntimeException() {
        doThrow(new RuntimeException("Invalid JSON")).when(factorySpy).loadMetaSchema(anyString(), any());
        assertThrows(RuntimeException.class, () -> factorySpy.getMetaSchema("http://example.com/schema/bad-json-meta-schema.json", new SchemaValidatorsConfig()), "Malformed JSON should be wrapped and re‑thrown as a RuntimeException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentLoadException_isPropagatedToAllThreads() throws InterruptedException {
        final String iri = "http://example.com/schema/concurrent-failure-meta-schema.json";
        final RuntimeException failure = new RuntimeException("load failure");
        doThrow(failure).when(factorySpy).loadMetaSchema(anyString(), any());
        final int threads = 10;
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicBoolean allFailed = new AtomicBoolean(true);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    factorySpy.getMetaSchema(iri, new SchemaValidatorsConfig());
                    // should not reach here
                    allFailed.set(false);
                } catch (RuntimeException e) {
                    assertSame(failure, e, "All threads must receive the same exception instance");
                }
            }).start();
        }
        ready.await();
        start.countDown();
        // small pause to let threads finish
        Thread.sleep(500);
        assertTrue(allFailed.get(), "All threads must have thrown the expected exception");
        // loadMetaSchema must have been invoked only once despite many callers.
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), any());
    }
}
