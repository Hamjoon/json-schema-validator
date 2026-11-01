```java
package com.networknt.schema;

import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    // ----- shared test fixtures -------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private SchemaLoaders.Builder schemaLoadersBuilderMock;
    @Mock
    private SchemaMappers.Builder schemaMappersBuilderMock;
    @Mock
    private JsonMetaSchema metaSchemaMock;

    private Map<String, JsonMetaSchema> metaSchemaMap;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a map containing the default meta‑schema – the factory validates this
        metaSchemaMap = new HashMap<>();
        // The key must be the *normalized* form of the default URI.
        // The method normalizeMetaSchemaUri is protected static, so we invoke it via reflection.
        try {
            java.lang.reflect.Method normMethod = JsonSchemaFactory.class
                    .getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            normMethod.setAccessible(true);
            String normalized = (String) normMethod.invoke(null, DEFAULT_META_SCHEMA_URI);
            metaSchemaMap.put(normalized, metaSchemaMock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – all mocks are reset automatically by MockitoExtension
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -------------------------------------------------------------------------
    // Helper: create a JsonSchemaFactory instance via the private constructor.
    // -------------------------------------------------------------------------
    private JsonSchemaFactory createFactory(boolean enableCache) throws Exception {
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class,
                        ObjectMapper.class,
                        String.class,
                        SchemaLoaders.Builder.class,
                        SchemaMappers.Builder.class,
                        Map.class,
                        boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                schemaLoadersBuilderMock,
                schemaMappersBuilderMock,
                metaSchemaMap,
                enableCache);
    }

    // -------------------------------------------------------------------------
    // Test 1 – cache enabled: the underlying getMappedSchema() must be invoked
    // only once, subsequent calls must reuse the cached JsonSchema.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_ShouldReuseCachedSchema() throws Exception {
        // Arrange
        JsonSchemaFactory realFactory = createFactory(true);
        JsonSchemaFactory factorySpy = Mockito.spy(realFactory);

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig();

        JsonSchema mappedSchema = mock(JsonSchema.class);
        // The factory returns the same mapped schema for every call to getMappedSchema()
        doReturn(mappedSchema).when(factorySpy).getMappedSchema(any(SchemaLocation.class), any());

        // withConfig() must return the schema itself (the real implementation does this)
        when(mappedSchema.withConfig(any(SchemaValidatorsConfig.class))).thenReturn(mappedSchema);

        // Act – first call
        JsonSchema result1 = factorySpy.getSchema(schemaLocation, cfg1);
        // Act – second call with a *different* config but same location
        JsonSchema result2 = factorySpy.getSchema(schemaLocation, cfg2);

        // Assert
        assertSame(mappedSchema, result1, "First call should return the mapped schema");
        assertSame(mappedSchema, result2, "Second call should return the cached schema");

        // getMappedSchema must be called only once
        verify(factorySpy, times(1)).getMappedSchema(eq(schemaLocation), any());

        // withConfig must be called twice – once for each config supplied
        verify(mappedSchema, times(2)).withConfig(any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // Test 2 – cache disabled: each invocation must delegate to getMappedSchema().
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithoutCache_ShouldCallMappedSchemaEveryTime() throws Exception {
        // Arrange
        JsonSchemaFactory realFactory = createFactory(false);
        JsonSchemaFactory factorySpy = Mockito.spy(realFactory);

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema mappedSchema1 = mock(JsonSchema.class);
        JsonSchema mappedSchema2 = mock(JsonSchema.class);

        // Return different mocks for successive calls to illustrate that the method is hit each time
        doReturn(mappedSchema1).doReturn(mappedSchema2)
                .when(factorySpy).getMappedSchema(eq(schemaLocation), any());

        when(mappedSchema1.withConfig(any())).thenReturn(mappedSchema1);
        when(mappedSchema2.withConfig(any())).thenReturn(mappedSchema2);

        // Act – first call
        JsonSchema result1 = factorySpy.getSchema(schemaLocation, cfg);
        // Act – second call
        JsonSchema result2 = factorySpy.getSchema(schemaLocation, cfg);

        // Assert
        assertSame(mappedSchema1, result1, "First call should return the first mapped schema");
        assertSame(mappedSchema2, result2, "Second call should return the second mapped schema");

        // getMappedSchema must be called twice (once per invocation)
        verify(factorySpy, times(2)).getMappedSchema(eq(schemaLocation), any());

        // withConfig must be called once per result
        verify(mappedSchema1, times(1)).withConfig(any());
        verify(mappedSchema2, times(1)).withConfig(any());
    }

    // -------------------------------------------------------------------------
    // Test 3 – cache disabled: withConfig must NOT be called because the
    // factory returns the raw schema produced by getMappedSchema().
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithoutCache_ShouldNotCallWithConfig() throws Exception {
        // Arrange
        JsonSchemaFactory factory = createFactory(false);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        JsonSchema rawSchema = mock(JsonSchema.class);
        // getMappedSchema returns a pre‑configured schema (real implementation does that)
        doReturn(rawSchema).when(spy).getMappedSchema(eq(location), eq(config));

        // Act
        JsonSchema result = spy.getSchema(location, config);

        // Assert
        assertSame(rawSchema, result, "Returned schema should be the raw schema from getMappedSchema()");
        // withConfig must never be invoked on the returned schema
        verify(rawSchema, never()).withConfig(any());
    }

    // -------------------------------------------------------------------------
    // Test 4 – cache enabled – first miss: getMappedSchema invoked once,
    // result cached, withConfig called.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_FirstMiss_ShouldCacheAndConfigure() throws Exception {
        // Arrange
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        JsonSchema rawSchema = mock(JsonSchema.class);
        JsonSchema configuredSchema = mock(JsonSchema.class);
        doReturn(rawSchema).when(spy).getMappedSchema(eq(location), eq(config));
        when(rawSchema.withConfig(eq(config))).thenReturn(configuredSchema);

        // Act
        JsonSchema result = spy.getSchema(location, config);

        // Assert
        assertSame(configuredSchema, result, "Result must be the configured schema");
        verify(spy, times(1)).getMappedSchema(eq(location), any());
        verify(rawSchema, times(1)).withConfig(eq(config));
    }

    // -------------------------------------------------------------------------
    // Test 5 – cache enabled – different key: second distinct key triggers another
    // getMappedSchema call.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_DifferentKey_ShouldInvokeMappedSchemaAgain() throws Exception {
        // Arrange
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation loc1 = mock(SchemaLocation.class);
        SchemaLocation loc2 = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema raw1 = mock(JsonSchema.class);
        JsonSchema raw2 = mock(JsonSchema.class);
        JsonSchema cfg1 = mock(JsonSchema.class);
        JsonSchema cfg2 = mock(JsonSchema.class);

        when(spy.getMappedSchema(eq(loc1), any())).thenReturn(raw1);
        when(spy.getMappedSchema(eq(loc2), any())).thenReturn(raw2);
        when(raw1.withConfig(eq(cfg))).thenReturn(cfg1);
        when(raw2.withConfig(eq(cfg))).thenReturn(cfg2);

        // Act – first distinct key
        JsonSchema result1 = spy.getSchema(loc1, cfg);
        // Act – second distinct key
        JsonSchema result2 = spy.getSchema(loc2, cfg);

        // Assert
        assertSame(cfg1, result1);
        assertSame(cfg2, result2);
        verify(spy, times(1)).getMappedSchema(eq(loc1), any());
        verify(spy, times(1)).getMappedSchema(eq(loc2), any());
    }

    // -------------------------------------------------------------------------
    // Test 6 – null SchemaLocation, cache disabled → NPE
    // -------------------------------------------------------------------------
    @Test
    void getSchema_NullLocation_NoCache_ShouldThrowNPE() throws Exception {
        JsonSchemaFactory factory = createFactory(false);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, cfg),
                "Calling getSchema with null location should throw NPE when cache is disabled");
    }

    // -------------------------------------------------------------------------
    // Test 7 – null SchemaLocation, cache enabled → NPE (computeIfAbsent)
    // -------------------------------------------------------------------------
    @Test
    void getSchema_NullLocation_WithCache_ShouldThrowNPE() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, cfg),
                "Calling getSchema with null location should throw NPE when cache is enabled");
    }

    // -------------------------------------------------------------------------
    // Test 8 – null Config parameter → NPE when withConfig(null) is invoked.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_NullConfig_ShouldThrowNPE() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        JsonSchema raw = mock(JsonSchema.class);
        doReturn(raw).when(spy).getMappedSchema(eq(location), isNull());

        // The real JsonSchema.withConfig(null) throws NPE; we let the mock behave the same.
        when(raw.withConfig(isNull())).thenThrow(NullPointerException.class);

        assertThrows(NullPointerException.class,
                () -> spy.getSchema(location, null),
                "Passing null config must result in NPE from withConfig(null)");
    }

    // -------------------------------------------------------------------------
    // Test 9 – exception propagation from getMappedSchema.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_GetMappedSchemaThrows_ExceptionPropagates() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        IOException ioEx = new IOException("simulated I/O failure");
        doThrow(ioEx).when(spy).getMappedSchema(eq(location), any());

        IOException thrown = assertThrows(IOException.class,
                () -> spy.getSchema(location, cfg),
                "IOException from getMappedSchema must propagate");
        assertSame(ioEx, thrown);
    }

    // -------------------------------------------------------------------------
    // Test 10 – concurrent first‑miss: getMappedSchema must be executed only once.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_ConcurrentFirstMiss_InvokesMappedSchemaOnce() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema raw = mock(JsonSchema.class);
        JsonSchema configured = mock(JsonSchema.class);
        // Simulate a small delay inside getMappedSchema to increase race‑window
        doAnswer(invocation -> {
            Thread.sleep(100);
            return raw;
        }).when(spy).getMappedSchema(eq(location), any());

        when(raw.withConfig(eq(cfg))).thenReturn(configured);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<JsonSchema> task = () -> {
            startLatch.await(); // ensure both threads start at the same moment
            return spy.getSchema(location, cfg);
        };
        Future<JsonSchema> f1 = executor.submit(task);
        Future<JsonSchema> f2 = executor.submit(task);
        startLatch.countDown(); // release both threads

        JsonSchema r1 = f1.get(1, TimeUnit.SECONDS);
        JsonSchema r2 = f2.get(1, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertSame(configured, r1);
        assertSame(configured, r2);
        // getMappedSchema must have been called only once
        verify(spy, times(1)).getMappedSchema(eq(location), any());
    }

    // -------------------------------------------------------------------------
    // Test 11 – withConfig returns a *different* instance; the factory must not
    // expose the raw cached schema.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_WithConfigReturnsNewInstance() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema raw = mock(JsonSchema.class);
        JsonSchema configured = mock(JsonSchema.class);
        doReturn(raw).when(spy).getMappedSchema(eq(location), any());
        when(raw.withConfig(eq(cfg))).thenReturn(configured);

        JsonSchema result = spy.getSchema(location, cfg);
        assertSame(configured, result, "Result must be the object returned by withConfig");
        // Ensure the cached value is the *raw* schema, not the configured one.
        // Access the private cache via reflection.
        java.lang.reflect.Field cacheField = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<SchemaLocation, JsonSchema> cache = (ConcurrentMap<SchemaLocation, JsonSchema>) cacheField.get(spy);
        assertSame(raw, cache.get(location), "Cache must contain the raw schema");
    }

    // -------------------------------------------------------------------------
    // Test 12 – changing config between calls while cache enabled.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_ChangingConfig_BetweenCalls() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfgA = new SchemaValidatorsConfig();
        cfgA.setFailFast(false);
        SchemaValidatorsConfig cfgB = new SchemaValidatorsConfig();
        cfgB.setFailFast(true);

        JsonSchema raw = mock(JsonSchema.class);
        JsonSchema configuredA = mock(JsonSchema.class);
        JsonSchema configuredB = mock(JsonSchema.class);

        doReturn(raw).when(spy).getMappedSchema(eq(location), any());
        when(raw.withConfig(eq(cfgA))).thenReturn(configuredA);
        when(raw.withConfig(eq(cfgB))).thenReturn(configuredB);

        JsonSchema resultA = spy.getSchema(location, cfgA);
        JsonSchema resultB = spy.getSchema(location, cfgB);

        assertSame(configuredA, resultA);
        assertSame(configuredB, resultB);
        // getMappedSchema must be called only once (raw schema cached)
        verify(spy, times(1)).getMappedSchema(eq(location), any());
        // withConfig must be called twice with different configs
        verify(raw, times(1)).withConfig(eq(cfgA));
        verify(raw, times(1)).withConfig(eq(cfgB));
    }

    // -------------------------------------------------------------------------
    // Test 13 – cache disabled – multiple sequential calls: each call hits
    // getMappedSchema and withConfig must NOT be invoked.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithoutCache_MultipleSequentialCalls_NoWithConfig() throws Exception {
        JsonSchemaFactory factory = createFactory(false);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        doReturn(schema1).doReturn(schema2).when(spy).getMappedSchema(eq(location), eq(cfg));

        // Act
        JsonSchema r1 = spy.getSchema(location, cfg);
        JsonSchema r2 = spy.getSchema(location, cfg);

        // Assert
        assertSame(schema1, r1);
        assertSame(schema2, r2);
        verify(spy, times(2)).getMappedSchema(eq(location), eq(cfg));
        verify(schema1, never()).withConfig(any());
        verify(schema2, never()).withConfig(any());
    }

    // -------------------------------------------------------------------------
    // Test 14 – cache key sensitivity with fragment identifiers.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_FragmentIdentifiers_TreatedAsDistinctKeys() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation fragA = mock(SchemaLocation.class);
        SchemaLocation fragB = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema rawA = mock(JsonSchema.class);
        JsonSchema rawB = mock(JsonSchema.class);
        JsonSchema cfgA = mock(JsonSchema.class);
        JsonSchema cfgB = mock(JsonSchema.class);

        when(spy.getMappedSchema(eq(fragA), any())).thenReturn(rawA);
        when(spy.getMappedSchema(eq(fragB), any())).thenReturn(rawB);
        when(rawA.withConfig(eq(cfg))).thenReturn(cfgA);
        when(rawB.withConfig(eq(cfg))).thenReturn(cfgB);

        JsonSchema rA = spy.getSchema(fragA, cfg);
        JsonSchema rB = spy.getSchema(fragB, cfg);

        assertSame(cfgA, rA);
        assertSame(cfgB, rB);
        verify(spy, times(1)).getMappedSchema(eq(fragA), any());
        verify(spy, times(1)).getMappedSchema(eq(fragB), any());
    }

    // -------------------------------------------------------------------------
    // Test 15 – cache enabled – getMappedSchema returns null → NPE.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_GetMappedSchemaReturnsNull_ShouldThrowNPE() throws Exception {
        JsonSchemaFactory factory = createFactory(true);
        JsonSchemaFactory spy = Mockito.spy(factory);

        SchemaLocation location = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        doReturn(null).when(spy).getMappedSchema(eq(location), any());

        // The subsequent withConfig call on a null reference will cause NPE.
        assertThrows(NullPointerException.class,
                () -> spy.getSchema(location, cfg),
                "If getMappedSchema returns null, getSchema must throw NPE");
    }
}
```