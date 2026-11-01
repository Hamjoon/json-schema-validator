```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#loadSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The tests cover the full matrix of scenarios described in the JSON test‑case
 * specification – cache enabled/disabled, cache hits/misses, null arguments,
 * exceptions from {@code getMappedSchema}, and behaviour with different
 * {@link SchemaValidatorsConfig} instances.</p>
 *
 * <p>We create a real {@link JsonSchemaFactory} via {@code getInstance(...)} and
 * turn it into a {@link Mockito#spy(Object)} so that the protected
 * {@code getMappedSchema(..)} method can be stubbed.  The returned {@link JsonSchema}
 * objects are also mocks.  The private {@code enableSchemaCache} flag and the
 * internal {@code schemaCache} map are manipulated via reflection to avoid any
 * reliance on private APIs.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadSchema_Test {

    /** Spy of the factory – allows stubbing {@code getMappedSchema}. */
    private JsonSchemaFactory factorySpy;

    /** A mock {@link JsonSchema} used in many tests (re‑created per test when needed). */
    @Mock
    private JsonSchema genericMappedSchemaMock;

    /** Simple config instance used as default. */
    private SchemaValidatorsConfig defaultConfig;

    /** Helper for invoking the protected {@code loadSchema} method. */
    private Method loadSchemaMethod;

    /** Reflection field for the {@code enableSchemaCache} flag. */
    private Field cacheFlagField;

    /** Reflection field for the internal {@code schemaCache} map. */
    private Field cacheMapField;

    @BeforeEach
    void setUp() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Create a real factory (cache disabled by default) and spy it.
        // -----------------------------------------------------------------
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012);
        factorySpy = Mockito.spy(realFactory);

        // -----------------------------------------------------------------
        // 2️⃣  Prepare default arguments.
        // -----------------------------------------------------------------
        defaultConfig = SchemaValidatorsConfig.builder().build();

        // -----------------------------------------------------------------
        // 3️⃣  Obtain reflection handles (once per test class for simplicity).
        // -----------------------------------------------------------------
        loadSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod(
                "loadSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
        loadSchemaMethod.setAccessible(true);

        cacheFlagField = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
        cacheFlagField.setAccessible(true);
        cacheMapField = JsonSchemaFactory.class.getDeclaredField("schemaCache");
        cacheMapField.setAccessible(true);
    }

    /** Helper to invoke the protected method on the spy. */
    private JsonSchema invokeLoadSchema(SchemaLocation location,
                                        SchemaValidatorsConfig cfg) throws Exception {
        return (JsonSchema) loadSchemaMethod.invoke(factorySpy, location, cfg);
    }

    /** Utility to (re)initialise the internal cache with an empty map. */
    private void resetCacheMap() throws IllegalAccessException {
        cacheMapField.set(factorySpy, new ConcurrentHashMap<>());
    }

    /** Utility to set the {@code enableSchemaCache} flag. */
    private void setCacheEnabled(boolean enabled) throws IllegalAccessException {
        cacheFlagField.setBoolean(factorySpy, enabled);
    }

    // -----------------------------------------------------------------
    // Existing tests (kept unchanged)
    // -----------------------------------------------------------------

    /**
     * When the internal cache is enabled the factory must call
     * {@code getMappedSchema} only once for the same {@link SchemaLocation}
     * and subsequently return the cached instance (still wrapped with the
     * supplied config).
     */
    @Test
    void loadSchema_cacheEnabled_callsGetMappedSchemaOnlyOnce() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/test-schema");
        JsonSchema mappedSchemaMock = mock(JsonSchema.class);
        when(mappedSchemaMock.withConfig(eq(defaultConfig))).thenReturn(mappedSchemaMock);
        doReturn(mappedSchemaMock).when(factorySpy)
                .getMappedSchema(eq(schemaLocation), eq(defaultConfig));

        JsonSchema first = invokeLoadSchema(schemaLocation, defaultConfig);
        assertSame(mappedSchemaMock, first, "First call should return the mocked schema");

        JsonSchema second = invokeLoadSchema(schemaLocation, defaultConfig);
        assertSame(mappedSchemaMock, second, "Second call should return the same cached schema");

        verify(factorySpy, times(1)).getMappedSchema(eq(schemaLocation), eq(defaultConfig));
        verify(mappedSchemaMock, times(2)).withConfig(eq(defaultConfig));
    }

    /**
     * When the cache is disabled the factory must delegate to
     * {@code getMappedSchema} for every invocation.
     */
    @Test
    void loadSchema_cacheDisabled_callsGetMappedSchemaEveryTime() throws Exception {
        setCacheEnabled(false);

        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/test-schema");
        JsonSchema mappedSchemaMock = mock(JsonSchema.class);
        when(mappedSchemaMock.withConfig(eq(defaultConfig))).thenReturn(mappedSchemaMock);
        doReturn(mappedSchemaMock).when(factorySpy)
                .getMappedSchema(eq(schemaLocation), eq(defaultConfig));

        JsonSchema first = invokeLoadSchema(schemaLocation, defaultConfig);
        assertSame(mappedSchemaMock, first, "First call should return the mocked schema");

        JsonSchema second = invokeLoadSchema(schemaLocation, defaultConfig);
        assertSame(mappedSchemaMock, second, "Second call should also return the mocked schema");

        verify(factorySpy, times(2)).getMappedSchema(eq(schemaLocation), eq(defaultConfig));
        verify(mappedSchemaMock, times(2)).withConfig(eq(defaultConfig));
    }

    // -----------------------------------------------------------------
    // Additional tests – covering every JSON test‑case group
    // -----------------------------------------------------------------

    /** Cache disabled – direct mapping returns a non‑null schema. */
    @Test
    void cacheDisabled_directMappingReturnsSchema() throws Exception {
        setCacheEnabled(false);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/schema1.json");
        JsonSchema mapped = mock(JsonSchema.class);
        when(mapped.withConfig(any())).thenReturn(mapped);
        doReturn(mapped).when(factorySpy).getMappedSchema(eq(uri), any());

        JsonSchema result = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(mapped, result);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any());
    }

    /** Cache disabled – direct mapping returns null (propagated). */
    @Test
    void cacheDisabled_directMappingReturnsNull() throws Exception {
        setCacheEnabled(false);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/unknown.json");
        doReturn(null).when(factorySpy).getMappedSchema(eq(uri), any());

        JsonSchema result = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertNull(result, "Null from getMappedSchema must be propagated");
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any());
    }

    /** Cache enabled – first call caches a non‑null schema. */
    @Test
    void cacheEnabled_cacheMissMappingSucceeds() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/schema2.json");
        JsonSchema mapped = mock(JsonSchema.class);
        when(mapped.withConfig(any())).thenReturn(mapped);
        doReturn(mapped).when(factorySpy).getMappedSchema(eq(uri), any());

        JsonSchema first = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(mapped, first);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any());

        // second call – should hit cache
        JsonSchema second = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(mapped, second);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any()); // still only once
    }

    /** Cache enabled – cache miss but mapping returns null (no cache entry). */
    @Test
    void cacheEnabled_cacheMissMappingReturnsNull() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/invalid.json");
        doReturn(null).when(factorySpy).getMappedSchema(eq(uri), any());

        assertThrows(NullPointerException.class,
                () -> invokeLoadSchema(uri, new SchemaValidatorsConfig()),
                "Null result should cause NPE when withConfig is invoked");

        // Verify that the cache still does not contain the key
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        assertFalse(cache.containsKey(uri), "Cache must not store a null entry");
    }

    /** Cache enabled – simulate race where cache is checked twice. */
    @Test
    void cacheEnabled_concurrentLikeRecheckPath() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/schema3.json");
        JsonSchema mapped = mock(JsonSchema.class);
        when(mapped.withConfig(any())).thenReturn(mapped);

        // When getMappedSchema is called, we insert the same schema into the cache
        // to simulate another thread having populated it already.
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConcurrentMap<SchemaLocation, JsonSchema> cache =
                    (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
            cache.put(uri, mapped);
            return mapped;
        }).when(factorySpy).getMappedSchema(eq(uri), any());

        JsonSchema result = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(mapped, result);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any());

        // Ensure the cache contains exactly one entry.
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        assertEquals(1, cache.size());
        assertSame(mapped, cache.get(uri));
    }

    /** Cache enabled – cache hit with a different config (no extra mapping). */
    @Test
    void cacheEnabled_cacheHitDifferentConfig() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/schema4.json");
        JsonSchema cached = mock(JsonSchema.class);
        when(cached.withConfig(any())).thenAnswer(inv -> inv.getArgument(0)); // return config itself for visibility

        // Pre‑populate cache with the mocked schema.
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        cache.put(uri, cached);

        // First call with config A
        SchemaValidatorsConfig configA = SchemaValidatorsConfig.builder().build();
        JsonSchema resultA = invokeLoadSchema(uri, configA);
        assertSame(cached, resultA);
        verify(factorySpy, never()).getMappedSchema(any(), any());

        // Second call with config B
        SchemaValidatorsConfig configB = SchemaValidatorsConfig.builder().setFailFast(true).build();
        JsonSchema resultB = invokeLoadSchema(uri, configB);
        assertSame(cached, resultB);
        verify(factorySpy, never()).getMappedSchema(any(), any());

        // Verify that withConfig was invoked for each distinct config.
        verify(cached, times(2)).withConfig(any());
        verify(cached).withConfig(eq(configA));
        verify(cached).withConfig(eq(configB));
    }

    /** Cache enabled – null config argument is tolerated. */
    @Test
    void cacheEnabled_nullConfigArgument() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/schema5.json");
        JsonSchema cached = mock(JsonSchema.class);
        when(cached.withConfig(isNull())).thenReturn(cached);
        // Pre‑populate cache
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        cache.put(uri, cached);

        JsonSchema result = invokeLoadSchema(uri, null);
        assertSame(cached, result);
        verify(cached, times(1)).withConfig(isNull());
        verify(factorySpy, never()).getMappedSchema(any(), any());
    }

    /** Cache disabled – null schemaUri leads to NPE from getMappedSchema. */
    @Test
    void cacheDisabled_nullSchemaUri() throws Exception {
        setCacheEnabled(false);
        resetCacheMap();

        // No stubbing needed – the spy will receive a null argument and
        // the real method (which we don't call) would throw NPE.
        assertThrows(InvocationTargetException.class, () -> {
            try {
                invokeLoadSchema(null, new SchemaValidatorsConfig());
            } catch (InvocationTargetException ite) {
                // unwrap the cause to verify it is NPE
                assertTrue(ite.getCause() instanceof NullPointerException);
                throw ite;
            }
        });
    }

    /** Cache enabled – pre‑populated cache entry, getMappedSchema never called. */
    @Test
    void cacheEnabled_prepopulatedCacheEntry() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/prepopulated.json");
        JsonSchema prepopulated = mock(JsonSchema.class);
        when(prepopulated.withConfig(any())).thenReturn(prepopulated);

        // Insert into cache before the call.
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        cache.put(uri, prepopulated);

        JsonSchema result = invokeLoadSchema(uri, SchemaValidatorsConfig.builder().setReadOnly(true).build());
        assertSame(prepopulated, result);
        verify(factorySpy, never()).getMappedSchema(any(), any());
        verify(prepopulated, times(1)).withConfig(any());
    }

    /** Cache enabled – first mapping succeeds, second call sees null from getMappedSchema but returns cached. */
    @Test
    void cacheEnabled_flipFlopMapping() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/flipflop.json");
        JsonSchema firstSchema = mock(JsonSchema.class);
        when(firstSchema.withConfig(any())).thenReturn(firstSchema);

        // First call returns a real schema.
        doReturn(firstSchema).when(factorySpy).getMappedSchema(eq(uri), any());

        JsonSchema first = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(firstSchema, first);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any());

        // Change stub to return null for subsequent calls.
        doReturn(null).when(factorySpy).getMappedSchema(eq(uri), any());

        // Second call should still return the cached schema (and not throw NPE because we
        // never invoke withConfig on the null return; the code returns cached.withConfig).
        JsonSchema second = invokeLoadSchema(uri, new SchemaValidatorsConfig());
        assertSame(firstSchema, second);
        verify(factorySpy, times(1)).getMappedSchema(eq(uri), any()); // still only the first call
    }

    /** Cache enabled – null schemaUri causes NPE before any mapping. */
    @Test
    void cacheEnabled_nullSchemaUri() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        assertThrows(InvocationTargetException.class, () -> {
            try {
                invokeLoadSchema(null, new SchemaValidatorsConfig());
            } catch (InvocationTargetException ite) {
                assertTrue(ite.getCause() instanceof NullPointerException);
                throw ite;
            }
        });
    }

    /** Cache enabled – getMappedSchema returns null leading to NPE on withConfig. */
    @Test
    void cacheEnabled_getMappedSchemaReturnsNull_NPE() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/returns-null.json");
        doReturn(null).when(factorySpy).getMappedSchema(eq(uri), any());

        assertThrows(InvocationTargetException.class, () -> {
            try {
                invokeLoadSchema(uri, new SchemaValidatorsConfig());
            } catch (InvocationTargetException ite) {
                assertTrue(ite.getCause() instanceof NullPointerException);
                throw ite;
            }
        });
        // No entry should be cached.
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache =
                (ConcurrentMap<SchemaLocation, JsonSchema>) cacheMapField.get(factorySpy);
        assertFalse(cache.containsKey(uri));
    }

    /** Cache enabled – getMappedSchema throws unchecked IllegalStateException (propagated). */
    @Test
    void cacheEnabled_getMappedSchemaThrowsUncheckedException() throws Exception {
        setCacheEnabled(true);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/throws.json");
        doThrow(new IllegalStateException("boom")).when(factorySpy).getMappedSchema(eq(uri), any());

        IllegalStateException ex = assertThrows(InvocationTargetException.class, () -> {
            try {
                invokeLoadSchema(uri, new SchemaValidatorsConfig());
            } catch (InvocationTargetException ite) {
                assertTrue(ite.getCause() instanceof IllegalStateException);
                throw ite;
            }
        }).getCause();
        assertEquals("boom", ex.getMessage());
    }

    /** Cache disabled – getMappedSchema throws unchecked IllegalArgumentException (propagated). */
    @Test
    void cacheDisabled_getMappedSchemaThrowsUncheckedException() throws Exception {
        setCacheEnabled(false);
        resetCacheMap();

        SchemaLocation uri = SchemaLocation.of("http://example.com/throws-no-cache.json");
        doThrow(new IllegalArgumentException("invalid")).when(factorySpy).getMappedSchema(eq(uri), any());

        IllegalArgumentException ex = assertThrows(InvocationTargetException.class, () -> {
            try {
                invokeLoadSchema(uri, new SchemaValidatorsConfig());
            } catch (InvocationTargetException ite) {
                assertTrue(ite.getCause() instanceof IllegalArgumentException);
                throw ite;
            }
        }).getCause();
        assertEquals("invalid", ex.getMessage());
    }
}
```