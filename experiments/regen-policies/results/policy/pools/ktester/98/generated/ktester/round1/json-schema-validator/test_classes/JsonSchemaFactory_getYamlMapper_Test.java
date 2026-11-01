package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.YamlMapperFactory;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getYamlMapper_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    JsonNodeReader jsonNodeReader;

    @Mock
    SchemaLoaders.Builder schemaLoadersBuilder;

    @Mock
    SchemaMappers.Builder schemaMappersBuilder;

    @Mock
    JsonMetaSchemaFactory metaSchemaFactory;

    private static final String DEFAULT_META_SCHEMA_IRI = "https://json-schema.org/draft/2020-12/schema";

    /**
     * Helper that creates a {@link JsonSchemaFactory} instance via reflection.
     *
     * @param yamlMapper the mapper that should be stored inside the factory
     * @return a fully‑initialised factory
     * @throws Exception if reflection fails
     */
    private JsonSchemaFactory createFactory(ObjectMapper yamlMapper) throws Exception {
        // Minimal but valid arguments for the private constructor.
        // not used by the test
        ObjectMapper jsonMapper = new ObjectMapper();
        Map<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<>();
        // The factory validates that the map contains the default meta‑schema IRI.
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_IRI), mock(JsonMetaSchema.class));
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, ConcurrentHashMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper, yamlMapper, jsonNodeReader, DEFAULT_META_SCHEMA_IRI, schemaLoadersBuilder, schemaMappersBuilder, new ConcurrentHashMap<>(metaSchemas), false, metaSchemaFactory);
    }

    @Test
    void getYamlMapper_returnsCustomMapperWhenSet() throws Exception {
        // GIVEN a factory that was constructed with a non‑null yaml mapper
        ObjectMapper customYamlMapper = new ObjectMapper();
        JsonSchemaFactory factory = createFactory(customYamlMapper);
        // WHEN the yaml mapper is requested
        ObjectMapper result = factory.getYamlMapper();
        // THEN the same instance that was supplied at construction is returned
        assertSame(customYamlMapper, result, "Factory should return the yaml mapper supplied at construction");
    }

    @Test
    void getYamlMapper_returnsDefaultWhenNull() throws Exception {
        // GIVEN a factory created via the public API – it does not receive a yaml mapper
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // AND a mocked static YamlMapperFactory that returns a known mapper
        ObjectMapper defaultMapper = new ObjectMapper();
        try (MockedStatic<YamlMapperFactory> mocked = MockitoExtension.mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenReturn(defaultMapper);
            // WHEN the yaml mapper is requested
            ObjectMapper result = factory.getYamlMapper();
            // THEN the mapper provided by YamlMapperFactory is returned
            assertSame(defaultMapper, result, "Factory should delegate to YamlMapperFactory when its own yaml mapper is null");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_fallbackReturnsSameInstanceOnMultipleCalls() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        ObjectMapper fallback = new ObjectMapper();
        try (MockedStatic<YamlMapperFactory> mocked = mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenReturn(fallback);
            ObjectMapper first = factory.getYamlMapper();
            ObjectMapper second = factory.getYamlMapper();
            assertSame(fallback, first, "First call should return the mocked fallback mapper");
            assertSame(fallback, second, "Second call must return the same fallback instance");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_doesNotModifyInjectedMapper() throws Exception {
        ObjectMapper injected = new ObjectMapper();
        JsonSchemaFactory factory = createFactory(injected);
        // Capture the field value before the call
        Field field = JsonSchemaFactory.class.getDeclaredField("yamlMapper");
        field.setAccessible(true);
        ObjectMapper before = (ObjectMapper) field.get(factory);
        // Call the method
        ObjectMapper result = factory.getYamlMapper();
        // Verify the field still points to the same instance
        ObjectMapper after = (ObjectMapper) field.get(factory);
        assertSame(injected, before, "Field should initially hold the injected mapper");
        assertSame(injected, after, "Field must remain unchanged after getYamlMapper()");
        assertSame(injected, result, "Returned mapper must be the injected instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_nullFieldFallsBackToStaticProvider() throws Exception {
        // Build a factory with a non‑null mapper first
        JsonSchemaFactory factory = createFactory(new ObjectMapper());
        // Force the private field to null
        Field field = JsonSchemaFactory.class.getDeclaredField("yamlMapper");
        field.setAccessible(true);
        field.set(factory, null);
        ObjectMapper fallback = new ObjectMapper();
        try (MockedStatic<YamlMapperFactory> mocked = mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenReturn(fallback);
            ObjectMapper result = factory.getYamlMapper();
            assertSame(fallback, result, "When yamlMapper is null, fallback should be used");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_fallbackReturnsNull() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        try (MockedStatic<YamlMapperFactory> mocked = mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenReturn(null);
            ObjectMapper result = factory.getYamlMapper();
            assertNull(result, "If YamlMapperFactory returns null, getYamlMapper() should return null");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_fallbackThrowsRuntimeException() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        RuntimeException thrown = new IllegalStateException("fallback failure");
        try (MockedStatic<YamlMapperFactory> mocked = mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenThrow(thrown);
            RuntimeException ex = assertThrows(RuntimeException.class, factory::getYamlMapper, "Exception from YamlMapperFactory should propagate");
            assertSame(thrown, ex, "Propagated exception must be the same instance thrown by the fallback");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getYamlMapper_concurrentCallsReturnSameInstance() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        ObjectMapper fallback = new ObjectMapper();
        try (MockedStatic<YamlMapperFactory> mocked = mockStatic(YamlMapperFactory.class)) {
            mocked.when(YamlMapperFactory::getInstance).thenReturn(fallback);
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ConcurrentLinkedQueue<ObjectMapper> results = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        // ensure all threads start at the same time
                        startLatch.await();
                        results.add(factory.getYamlMapper());
                    } catch (Exception ignored) {
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            // Release all threads
            startLatch.countDown();
            // Wait for completion
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish in time");
            executor.shutdownNow();
            // Verify every collected instance is the same as the fallback
            for (ObjectMapper om : results) {
                assertSame(fallback, om, "All concurrent calls must return the same fallback instance");
            }
            assertEquals(threadCount, results.size(), "Exactly one result per thread should be collected");
        }
    }
}
