```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extensive unit tests for {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The test suite validates:</p>
 * <ul>
 *   <li>Cache‑miss behaviour (loading once, storing the result).</li>
 *   <li>Cache‑hit reuse (no second load).</li>
 *   <li>Normalization of the IRI (trailing “.json”, fragment, scheme case, trailing slash, etc.).</li>
 *   <li>Propagation of the {@link SchemaValidatorsConfig} argument.</li>
 *   <li>Exception handling for illegal IRIs and loading failures.</li>
 *   <li>Thread‑safety – concurrent calls must invoke {@code loadMetaSchema} only once.</li>
 * </ul>
 *
 * <p>All interactions with the protected {@code loadMetaSchema} method are verified by
 * spying on a real {@link JsonSchemaFactory} instance created via {@code getInstance}.
 * The method is stubbed to return a dummy {@link JsonMetaSchema} so that the tests
 * remain fast and do not depend on external resources.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    /** The factory under test – a Mockito spy so that we can verify calls to {@code loadMetaSchema}. */
    private JsonSchemaFactory factorySpy;

    /** A reusable dummy meta‑schema returned by the stubbed {@code loadMetaSchema}. */
    private JsonMetaSchema dummyMetaSchema;

    @BeforeEach
    void setUp() {
        // Create a real factory (any version works – we use V202012) and spy on it.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));

        factorySpy = Mockito.spy(realFactory);

        // Prepare a dummy meta‑schema that will be returned for every successful load.
        dummyMetaSchema = mock(JsonMetaSchema.class);
        // By default, loadMetaSchema returns the dummy instance.
        doReturn(dummyMetaSchema).when(factorySpy).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods                                                     */
    /* --------------------------------------------------------------------- */

    /**
     * Normalises an IRI using the same logic the production code uses.
     * The method is {@code protected static} – we invoke it via reflection.
     */
    private static String normalize(String iri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class
                    .getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, iri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Cache miss – load new meta‑schema                                 */
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
        ConcurrentMap<String, JsonMetaSchema> map = (ConcurrentMap<String, JsonMetaSchema>)
                getField(factorySpy, "metaSchemas");
        assertTrue(map.containsKey(expectedKey), "Internal cache should contain the normalised key");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Cache hit – reuse existing meta‑schema                           */
    /* --------------------------------------------------------------------- */
    @Test
    void cacheHit_reusesExistingInstance() {
        String iri = "http://example.com/schema/meta-schema.json";
        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig(); // different instance

        JsonMetaSchema first = factorySpy.getMetaSchema(iri, cfg1);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, cfg2);

        assertSame(first, second, "Subsequent calls must return the same cached instance");

        // loadMetaSchema must have been called only once (the first call).
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    /*  3. Normalisation effect – different IRIs map to same key           */
    /* --------------------------------------------------------------------- */
    @Test
    void normalisationDifferentIrisMapToSameKey() {
        String iri1 = "http://example.com/schema/meta-schema.json";
        String iri2 = "http://example.com/schema/meta-schema"; // no .json

        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema first = factorySpy.getMetaSchema(iri1, cfg);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri2, cfg);

        assertSame(first, second, "Both IRIs should resolve to the same cached meta‑schema");

        // loadMetaSchema must have been called only once because the normalised key is identical.
        verify(factorySpy, times(1)).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    /*  4. Null IRI handling                                                */
    /* --------------------------------------------------------------------- */
    @Test
    void nullIri_throwsException() {
        assertThrows(NullPointerException.class,
                () -> factorySpy.getMetaSchema(null, null),
                "Passing a null IRI must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /*  5. Config propagation – null config                                 */
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
    /*  6. Cache miss – custom SchemaValidatorsConfig is propagated         */
    /* --------------------------------------------------------------------- */
    @Test
    void customConfig_isForwardedOnCacheMiss() {
        String iri = "http://example.com/schema/custom-meta-schema.json";
        SchemaValidatorsConfig customCfg = new SchemaValidatorsConfig();
        customCfg.setTypeLoose(false); // a non‑default setting

        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        factorySpy.getMetaSchema(iri, customCfg);
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), cfgCaptor.capture());

        assertSame(customCfg, cfgCaptor.getValue(),
                "The exact custom config instance must be forwarded to loadMetaSchema");
    }

    /* --------------------------------------------------------------------- */
    /*  7. Cache hit – different config ignored after first load            */
    /* --------------------------------------------------------------------- */
    @Test
    void cacheHit_ignoresSubsequentConfigChanges() {
        String iri = "http://example.com/schema/custom-meta-schema.json";
        SchemaValidatorsConfig firstCfg = new SchemaValidatorsConfig();
        firstCfg.setTypeLoose(false);
        SchemaValidatorsConfig secondCfg = null; // different (null) config

        JsonMetaSchema first = factorySpy.getMetaSchema(iri, firstCfg);
        JsonMetaSchema second = factorySpy.getMetaSchema(iri, secondCfg);

        assertSame(first, second, "Cached instance must be returned irrespective of later config");

        // loadMetaSchema should have been called only once (the first call).
        verify(factorySpy, times(1)).loadMetaSchema(eq(normalize(iri)), any());
    }

    /* --------------------------------------------------------------------- */
    /*  8. Fragment identifier – same key as base IRI                        */
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
    /*  9. Case‑insensitivity of scheme – normalised key is lower‑cased    */
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
    /* 10. Empty string IRI – illegal argument handling                      */
    /* --------------------------------------------------------------------- */
    @Test
    void emptyStringIri_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getMetaSchema("", null),
                "Empty IRI should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Malformed IRI – parsing failure                                    */
    /* --------------------------------------------------------------------- */
    @Test
    void malformedIri_throwsException() {
        String badIri = "ht!tp://[invalid]";
        assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getMetaSchema(badIri, null),
                "Malformed IRI must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* 12. Trailing slash – treated as same meta‑schema                       */
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
    /* 13. Concurrent access – loadMetaSchema invoked only once             */
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
    /* 14. Load failure – unknown IRI                                         */
    /* --------------------------------------------------------------------- */
    @Test
    void unknownIri_loadFailsWithIllegalArgumentException() {
        // For this test we let the real implementation run (no stubbing) because
        // the stub would otherwise swallow the exception.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));

        assertThrows(IllegalArgumentException.class,
                () -> realFactory.getMetaSchema("urn:unknown:meta-schema", new SchemaValidatorsConfig()),
                "Unknown IRI must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* 15. Load failure – resource not found                                   */
    /* --------------------------------------------------------------------- */
    @Test
    void resourceNotFound_propagatesRuntimeException() {
        // Force loadMetaSchema to throw an unchecked exception.
        doThrow(new IllegalStateException("Resource not found"))
                .when(factorySpy).loadMetaSchema(anyString(), any());

        assertThrows(IllegalStateException.class,
                () -> factorySpy.getMetaSchema("http://example.com/schema/missing-meta-schema.json",
                        new SchemaValidatorsConfig()),
                "Missing resource should result in an unchecked exception");
    }

    /* --------------------------------------------------------------------- */
    /* 16. Load failure – malformed meta‑schema JSON                           */
    /* --------------------------------------------------------------------- */
    @Test
    void malformedMetaSchemaJson_propagatesRuntimeException() {
        doThrow(new RuntimeException("Invalid JSON"))
                .when(factorySpy).loadMetaSchema(anyString(), any());

        assertThrows(RuntimeException.class,
                () -> factorySpy.getMetaSchema("http://example.com/schema/bad-json-meta-schema.json",
                        new SchemaValidatorsConfig()),
                "Malformed JSON should be wrapped and re‑thrown as a RuntimeException");
    }

    /* --------------------------------------------------------------------- */
    /* 17. Concurrent load exception propagation                               */
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
                    allFailed.set(false); // should not reach here
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

    /* --------------------------------------------------------------------- */
    /*  Utility to read private fields via reflection                         */
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
}
```