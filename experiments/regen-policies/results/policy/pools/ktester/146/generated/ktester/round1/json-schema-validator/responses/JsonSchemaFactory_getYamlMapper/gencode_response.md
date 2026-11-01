```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.YamlMapperFactory;

/**
 * Unit tests for {@link JsonSchemaFactory#getYamlMapper()}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>Returning the injected {@code yamlMapper} when it is non‑null.</li>
 *   <li>Delegating to {@link YamlMapperFactory#getInstance()} when the field is {@code null}.</li>
 *   <li>Ensuring the method never returns {@code null}.</li>
 *   <li>Verifying that a {@link ClassCastException} is thrown if the field holds an
 *       object of the wrong type.</li>
 *   <li>Propagating exceptions thrown by {@code YamlMapperFactory.getInstance()}.</li>
 *   <li>Thread‑safety under concurrent access when the field is {@code null}.</li>
 * </ul>
 *
 * <p>The factory constructor is private; reflection is used to instantiate it.
 * All collaborators are mocked because they are irrelevant for the behaviour
 * under test.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getYamlMapper_Test {

    // -----------------------------------------------------------------------
    // Mocks that are shared by all tests
    // -----------------------------------------------------------------------
    @Mock
    JsonNodeReader jsonNodeReaderMock;
    @Mock
    SchemaLoaders.Builder schemaLoadersBuilderMock;
    @Mock
    SchemaMappers.Builder schemaMappersBuilderMock;
    @Mock
    JsonMetaSchemaFactory metaSchemaFactoryMock;
    @Mock
    JsonMetaSchema jsonMetaSchemaMock;

    // -----------------------------------------------------------------------
    // Constants used for constructing the factory
    // -----------------------------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_IRI = "http://example.com/meta";

    private static ConcurrentMap<String, JsonMetaSchema> metaSchemas;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the meta‑schema map once for the whole test suite.
        metaSchemas = new ConcurrentHashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri("http://example.com/meta"),
                mock(JsonMetaSchema.class));
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing specific per‑test; the mocks are injected by Mockito.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    /**
     * Helper that creates a {@link JsonSchemaFactory} instance via reflection.
     *
     * @param jsonMapper the JSON {@link ObjectMapper} to use (may be {@code null})
     * @param yamlMapper the YAML {@link ObjectMapper} to use (may be {@code null})
     * @return a fully constructed {@link JsonSchemaFactory}
     */
    private JsonSchemaFactory createFactory(ObjectMapper jsonMapper, ObjectMapper yamlMapper) {
        try {
            Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                    .getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class,
                            JsonNodeReader.class, String.class,
                            SchemaLoaders.Builder.class, SchemaMappers.Builder.class,
                            ConcurrentMap.class, boolean.class, JsonMetaSchemaFactory.class);
            ctor.setAccessible(true);
            return ctor.newInstance(jsonMapper, yamlMapper,
                    jsonNodeReaderMock, DEFAULT_META_SCHEMA_IRI,
                    schemaLoadersBuilderMock, schemaMappersBuilderMock,
                    metaSchemas, false, metaSchemaFactoryMock);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate JsonSchemaFactory via reflection", e);
        }
    }

    /**
     * Utility to replace the private {@code yamlMapper} field after construction.
     */
    private void setYamlMapperField(JsonSchemaFactory factory, Object value) {
        try {
            Field f = JsonSchemaFactory.class.getDeclaredField("yamlMapper");
            f.setAccessible(true);
            f.set(factory, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------
    // Existing scenarios (kept unchanged)
    // -----------------------------------------------------------------------

    @Test
    void getYamlMapper_returnsProvidedMapper_whenYamlMapperIsNotNull() {
        // Arrange
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        ObjectMapper yamlMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(jsonMapperMock, yamlMapperMock);

        // Act
        ObjectMapper result = factory.getYamlMapper();

        // Assert
        assertSame(yamlMapperMock, result,
                "When a yamlMapper is supplied to the factory, getYamlMapper() must return that instance");
    }

    @Test
    void getYamlMapper_returnsDefaultMapper_whenYamlMapperIsNull() {
        // Arrange
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(jsonMapperMock, null);

        // Mock the static call to YamlMapperFactory.getInstance()
        ObjectMapper defaultYamlMapperMock = mock(ObjectMapper.class);
        try (MockedStatic<YamlMapperFactory> yamlFactoryMock = mockStatic(YamlMapperFactory.class)) {
            yamlFactoryMock.when(YamlMapperFactory::getInstance).thenReturn(defaultYamlMapperMock);

            // Act
            ObjectMapper result = factory.getYamlMapper();

            // Assert
            assertSame(defaultYamlMapperMock, result,
                    "When yamlMapper is null, getYamlMapper() must delegate to YamlMapperFactory.getInstance()");
            yamlFactoryMock.verify(YamlMapperFactory::getInstance, times(1));
        }
    }

    // -----------------------------------------------------------------------
    // Additional test cases derived from the JSON specification
    // -----------------------------------------------------------------------

    @Test
    void getYamlMapper_neverReturnsNull() {
        // Two branches: non‑null yamlMapper and null yamlMapper.
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        ObjectMapper yamlMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factoryWithYaml = createFactory(jsonMapperMock, yamlMapperMock);
        JsonSchemaFactory factoryWithoutYaml = createFactory(jsonMapperMock, null);

        // Mock static fallback for the second factory.
        ObjectMapper defaultYamlMapper = mock(ObjectMapper.class);
        try (MockedStatic<YamlMapperFactory> yamlFactoryMock = mockStatic(YamlMapperFactory.class)) {
            yamlFactoryMock.when(YamlMapperFactory::getInstance).thenReturn(defaultYamlMapper);

            assertNotNull(factoryWithYaml.getYamlMapper(),
                    "Factory constructed with a yamlMapper must not return null");
            assertNotNull(factoryWithoutYaml.getYamlMapper(),
                    "Factory with null yamlMapper must still return a non‑null mapper from YamlMapperFactory");
        }
    }

    @Test
    void getYamlMapper_throwsClassCastException_whenFieldIsWrongType() {
        // Arrange: create factory with a *valid* yamlMapper (null) then replace it with a String.
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(jsonMapperMock, null);
        setYamlMapperField(factory, "I am not an ObjectMapper");

        // Act & Assert
        assertThrows(ClassCastException.class, factory::getYamlMapper,
                "If the yamlMapper field holds an object that is not an ObjectMapper, a ClassCastException must be thrown");
    }

    @Test
    void getYamlMapper_propagatesExceptionFromYamlMapperFactory() {
        // Arrange
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(jsonMapperMock, null);

        // Mock static method to throw IllegalStateException
        try (MockedStatic<YamlMapperFactory> yamlFactoryMock = mockStatic(YamlMapperFactory.class)) {
            yamlFactoryMock.when(YamlMapperFactory::getInstance)
                    .thenThrow(new IllegalStateException("factory failure"));

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class, factory::getYamlMapper,
                    "Exception from YamlMapperFactory.getInstance() must propagate unchanged");
            assertEquals("factory failure", ex.getMessage());
            yamlFactoryMock.verify(YamlMapperFactory::getInstance, times(1));
        }
    }

    @Test
    void getYamlMapper_concurrentAccess_whenYamlMapperIsNull() throws Exception {
        // Arrange: factory with null yamlMapper, static fallback returns a mock.
        ObjectMapper jsonMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(jsonMapperMock, null);
        ObjectMapper fallbackMapper = mock(ObjectMapper.class);

        try (MockedStatic<YamlMapperFactory> yamlFactoryMock = mockStatic(YamlMapperFactory.class)) {
            yamlFactoryMock.when(YamlMapperFactory::getInstance).thenReturn(fallbackMapper);

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            List<ObjectMapper> results = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // wait for the signal
                        ObjectMapper mapper = factory.getYamlMapper();
                        results.add(mapper);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Release all threads at once
            startLatch.countDown();
            // Wait for completion
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish within timeout");

            // Shut down executor
            executor.shutdownNow();

            // Assert no thread threw an exception
            assertTrue(errors.isEmpty(), "No thread should encounter an exception: " + errors);

            // Every result must be non‑null and equal to the fallback mapper
            assertEquals(threadCount, results.size(),
                    "All threads should have collected a result");
            for (ObjectMapper om : results) {
                assertNotNull(om, "Resulting mapper must not be null");
                assertSame(fallbackMapper, om, "All threads must receive the same fallback mapper");
            }

            // The static factory may be called more than once (implementation detail),
            // but it must be called at least once and never more than the number of threads.
            yamlFactoryMock.verify(YamlMapperFactory::getInstance, atLeastOnce());
            yamlFactoryMock.verify(YamlMapperFactory::getInstance, atMost(threadCount));
        }
    }
}
```