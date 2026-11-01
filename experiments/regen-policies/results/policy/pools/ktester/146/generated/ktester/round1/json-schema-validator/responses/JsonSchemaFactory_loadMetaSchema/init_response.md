```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code loadMetaSchema} method of {@link JsonSchemaFactory}.
 * <p>
 * The tests verify that the method delegates to the appropriate {@link JsonMetaSchemaFactory}
 * implementation depending on whether a custom factory is supplied or the default factory is used.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    /** Common test data */
    private static final String IRI = "https://example.com/meta/schema";
    private static final String DEFAULT_META_SCHEMA_IRI = "https://json-schema.org/draft/2020-12/schema";

    /** Mocks used across test cases */
    @Mock SchemaValidatorsConfig configMock;
    @Mock JsonMetaSchema metaSchemaMock;

    /** The factory instance under test – created via reflection because the constructor is private. */
    private JsonSchemaFactory factoryWithCustomFactory;
    private JsonSchemaFactory factoryWithNullFactory;

    /** Helper to invoke the protected method via reflection. */
    private Method loadMetaSchemaMethod;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // 1. Prepare the reflective constructor for JsonSchemaFactory.
        // ----------------------------------------------------------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
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

        // ----------------------------------------------------------------------
        // 2. Create a factory that uses a custom JsonMetaSchemaFactory (mocked).
        // ----------------------------------------------------------------------
        JsonMetaSchemaFactory customFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(customFactoryMock.getMetaSchema(eq(IRI), any(JsonSchemaFactory.class), eq(configMock)))
                .thenReturn(metaSchemaMock);

        factoryWithCustomFactory = ctor.newInstance(
                new ObjectMapper(),                     // jsonMapper
                new ObjectMapper(),                     // yamlMapper
                mock(JsonNodeReader.class),             // jsonNodeReader
                DEFAULT_META_SCHEMA_IRI,                // defaultMetaSchemaIri
                new SchemaLoaders.Builder(),            // schemaLoadersBuilder
                new SchemaMappers.Builder(),            // schemaMappersBuilder
                new ConcurrentHashMap<>(),              // metaSchemas (empty – will be populated lazily)
                true,                                   // enableSchemaCache
                customFactoryMock);                     // metaSchemaFactory (custom)

        // ----------------------------------------------------------------------
        // 3. Create a factory with a null meta‑schema factory (to trigger the default).
        // ----------------------------------------------------------------------
        factoryWithNullFactory = ctor.newInstance(
                new ObjectMapper(),
                new ObjectMapper(),
                mock(JsonNodeReader.class),
                DEFAULT_META_SCHEMA_IRI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                new ConcurrentHashMap<>(),
                true,
                null);                                 // metaSchemaFactory = null

        // ----------------------------------------------------------------------
        // 4. Obtain a reference to the protected loadMetaSchema method.
        // ----------------------------------------------------------------------
        loadMetaSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod(
                "loadMetaSchema", String.class, SchemaValidatorsConfig.class);
        loadMetaSchemaMethod.setAccessible(true);
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
     * Verify that when a custom {@link JsonMetaSchemaFactory} is supplied,
     * {@code loadMetaSchema} delegates to it and returns the value it provides.
     */
    @Test
    void loadMetaSchema_usesCustomMetaSchemaFactory() throws Exception {
        // Act
        JsonMetaSchema result = (JsonMetaSchema) loadMetaSchemaMethod.invoke(
                factoryWithCustomFactory, IRI, configMock);

        // Assert
        assertSame(metaSchemaMock, result, "The returned JsonMetaSchema should be the one supplied by the custom factory");
        // Verify that the custom factory was called exactly once with the expected arguments.
        verify(factoryWithCustomFactory.metaSchemaFactory, times(1))
                .getMetaSchema(eq(IRI), eq(factoryWithCustomFactory), eq(configMock));
    }

    /**
     * Verify that when no custom factory is present, the method falls back to
     * {@link DefaultJsonMetaSchemaFactory#getInstance()} and uses that instance.
     */
    @Test
    void loadMetaSchema_usesDefaultMetaSchemaFactoryWhenCustomIsNull() throws Exception {
        // Mock the static call to DefaultJsonMetaSchemaFactory.getInstance()
        JsonMetaSchemaFactory defaultFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(defaultFactoryMock.getMetaSchema(eq(IRI), any(JsonSchemaFactory.class), eq(configMock)))
                .thenReturn(metaSchemaMock);

        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance)
                    .thenReturn(defaultFactoryMock);

            // Act
            JsonMetaSchema result = (JsonMetaSchema) loadMetaSchemaMethod.invoke(
                    factoryWithNullFactory, IRI, configMock);

            // Assert
            assertSame(metaSchemaMock, result, "The returned JsonMetaSchema should be the one supplied by the default factory");
            // Verify delegation to the default factory.
            verify(defaultFactoryMock, times(1))
                    .getMetaSchema(eq(IRI), eq(factoryWithNullFactory), eq(configMock));
        }
    }
}
```