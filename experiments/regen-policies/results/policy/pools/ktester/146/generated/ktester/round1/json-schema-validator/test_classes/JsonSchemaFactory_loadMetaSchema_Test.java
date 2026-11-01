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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    /**
     * Common test data
     */
    private static final String IRI = "https://example.com/meta/schema";

    private static final String DEFAULT_META_SCHEMA_IRI = "https://json-schema.org/draft/2020-12/schema";

    /**
     * Mocks used across test cases
     */
    @Mock
    SchemaValidatorsConfig configMock;

    @Mock
    JsonMetaSchema metaSchemaMock;

    /**
     * The factory instance under test – created via reflection because the constructor is private.
     */
    private JsonSchemaFactory factoryWithCustomFactory;

    private JsonSchemaFactory factoryWithNullFactory;

    /**
     * Helper to invoke the protected method via reflection.
     */
    private Method loadMetaSchemaMethod;

    /**
     * The custom factory mock – needed for verification
     */
    private JsonMetaSchemaFactory customFactoryMock;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // 1. Prepare the reflective constructor for JsonSchemaFactory.
        // ----------------------------------------------------------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, ConcurrentMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        // ----------------------------------------------------------------------
        // 2. Create a factory that uses a custom JsonMetaSchemaFactory (mocked).
        // ----------------------------------------------------------------------
        JsonMetaSchemaFactory customFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(customFactoryMock.getMetaSchema(eq(IRI), any(JsonSchemaFactory.class), eq(configMock))).thenReturn(metaSchemaMock);
        factoryWithCustomFactory = ctor.newInstance(// jsonMapper
        new ObjectMapper(), // yamlMapper
        new ObjectMapper(), // jsonNodeReader
        mock(JsonNodeReader.class), // defaultMetaSchemaIri
        DEFAULT_META_SCHEMA_IRI, // schemaLoadersBuilder
        new SchemaLoaders.Builder(), // schemaMappersBuilder
        new SchemaMappers.Builder(), // metaSchemas (empty – will be populated lazily)
        new ConcurrentHashMap<>(), // enableSchemaCache
        true, // metaSchemaFactory (custom)
        customFactoryMock);
        // ----------------------------------------------------------------------
        // 3. Create a factory with a null meta‑schema factory (to trigger the default).
        // ----------------------------------------------------------------------
        factoryWithNullFactory = ctor.newInstance(new ObjectMapper(), new ObjectMapper(), mock(JsonNodeReader.class), DEFAULT_META_SCHEMA_IRI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), new ConcurrentHashMap<>(), true, // metaSchemaFactory = null
        null);
        // ----------------------------------------------------------------------
        // 4. Obtain a reference to the protected loadMetaSchema method.
        // ----------------------------------------------------------------------
        loadMetaSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod("loadMetaSchema", String.class, SchemaValidatorsConfig.class);
        loadMetaSchemaMethod.setAccessible(true);
        // ----------------------------------------------------------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, ConcurrentHashMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        // ----------------------------------------------------------------------
        customFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(customFactoryMock.getMetaSchema(anyString(), any(JsonSchemaFactory.class), any())).thenReturn(// default behaviour
        metaSchemaMock);
        factoryWithCustomFactory = ctor.newInstance(// jsonMapper
        new ObjectMapper(), // yamlMapper
        new ObjectMapper(), // jsonNodeReader
        mock(JsonNodeReader.class), // defaultMetaSchemaIri
        DEFAULT_META_SCHEMA_IRI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), new ConcurrentHashMap<>(), true, // custom meta‑schema factory
        customFactoryMock);
        // ----------------------------------------------------------------------
        factoryWithNullFactory = ctor.newInstance(new ObjectMapper(), new ObjectMapper(), mock(JsonNodeReader.class), DEFAULT_META_SCHEMA_IRI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), new ConcurrentHashMap<>(), true, // metaSchemaFactory = null
        null);
        // ----------------------------------------------------------------------
        loadMetaSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod("loadMetaSchema", String.class, SchemaValidatorsConfig.class);
        loadMetaSchemaMethod.setAccessible(true);
    }

    /* ----------------------------------------------------------------------
     * Helper to invoke the protected method.
     * ---------------------------------------------------------------------- */
    private JsonMetaSchema invokeLoadMetaSchema(JsonSchemaFactory factory, String iri, SchemaValidatorsConfig cfg) throws Exception {
        return (JsonMetaSchema) loadMetaSchemaMethod.invoke(factory, iri, cfg);
    }

    private static Stream<Arguments> customFactoryDelegateProvider() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        return Stream.of(// normal non‑null arguments
        Arguments.of("http://example.com/meta", cfg, mock(JsonMetaSchema.class)), // both arguments null
        Arguments.of(null, null, mock(JsonMetaSchema.class)));
    }

    private static Stream<Arguments> defaultFactoryDelegateProvider() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        return Stream.of(// non‑null iri & config
        Arguments.of("http://example.com/default", cfg, mock(JsonMetaSchema.class)), // non‑null iri & null config
        Arguments.of("urn:jsonschema:meta", null, mock(JsonMetaSchema.class)));
    }

    /**
     * Verify that when a custom {@link JsonMetaSchemaFactory} is supplied,
     * {@code loadMetaSchema} delegates to it and returns the value it provides.
     */
    @Test
    void loadMetaSchema_usesCustomMetaSchemaFactory() throws Exception {
        // Act
        JsonMetaSchema result = (JsonMetaSchema) loadMetaSchemaMethod.invoke(factoryWithCustomFactory, IRI, configMock);
        // Assert
        assertSame(metaSchemaMock, result, "The returned JsonMetaSchema should be the one supplied by the custom factory");
        // Verify that the custom factory was called exactly once with the expected arguments.
        verify(factoryWithCustomFactory.metaSchemaFactory, times(1)).getMetaSchema(eq(IRI), eq(factoryWithCustomFactory), eq(configMock));
    }

    /**
     * Verify that when no custom factory is present, the method falls back to
     * {@link DefaultJsonMetaSchemaFactory#getInstance()} and uses that instance.
     */
    @Test
    void loadMetaSchema_usesDefaultMetaSchemaFactoryWhenCustomIsNull() throws Exception {
        // Mock the static call to DefaultJsonMetaSchemaFactory.getInstance()
        JsonMetaSchemaFactory defaultFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(defaultFactoryMock.getMetaSchema(eq(IRI), any(JsonSchemaFactory.class), eq(configMock))).thenReturn(metaSchemaMock);
        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance).thenReturn(defaultFactoryMock);
            // Act
            JsonMetaSchema result = (JsonMetaSchema) loadMetaSchemaMethod.invoke(factoryWithNullFactory, IRI, configMock);
            // Assert
            assertSame(metaSchemaMock, result, "The returned JsonMetaSchema should be the one supplied by the default factory");
            // Verify delegation to the default factory.
            verify(defaultFactoryMock, times(1)).getMetaSchema(eq(IRI), eq(factoryWithNullFactory), eq(configMock));
        }
    }

    /* ----------------------------------------------------------------------
     * GROUP 1: metaSchemaFactory is non‑null – delegate to custom factory
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] iri={0}, config={1}")
    @MethodSource("customFactoryDelegateProvider")
    void loadMetaSchema_customFactoryDelegates(String iri, SchemaValidatorsConfig cfg, JsonMetaSchema expected) throws Exception {
        // arrange – specific behaviour for the current parameters
        reset(customFactoryMock);
        when(customFactoryMock.getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg))).thenReturn(expected);
        // act
        JsonMetaSchema result = invokeLoadMetaSchema(factoryWithCustomFactory, iri, cfg);
        // assert
        assertSame(expected, result, "The method should return exactly the JsonMetaSchema supplied by the custom factory");
        verify(customFactoryMock, times(1)).getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg));
    }

    /* ----------------------------------------------------------------------
     * GROUP 2: metaSchemaFactory is non‑null – factory returns null
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_customFactoryReturnsNull() throws Exception {
        String iri = "http://example.com/return-null";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        reset(customFactoryMock);
        when(customFactoryMock.getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg))).thenReturn(null);
        JsonMetaSchema result = invokeLoadMetaSchema(factoryWithCustomFactory, iri, cfg);
        assertNull(result, "When the custom factory returns null, loadMetaSchema must propagate null");
    }

    /* ----------------------------------------------------------------------
     * GROUP 3: metaSchemaFactory is non‑null – factory throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_customFactoryThrowsRuntimeException() {
        String iri = "http://example.com/exception";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        reset(customFactoryMock);
        when(customFactoryMock.getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeLoadMetaSchema(factoryWithCustomFactory, iri, cfg));
        assertEquals("boom", ex.getMessage());
    }

    /* ----------------------------------------------------------------------
     * GROUP 4: metaSchemaFactory is non‑null – factory throws IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_customFactoryThrowsIllegalArgumentException() {
        String iri = "not a valid uri";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        reset(customFactoryMock);
        when(customFactoryMock.getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg))).thenThrow(new IllegalArgumentException("bad IRI"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> invokeLoadMetaSchema(factoryWithCustomFactory, iri, cfg));
        assertEquals("bad IRI", ex.getMessage());
    }

    /* ----------------------------------------------------------------------
     * GROUP 5: metaSchemaFactory is non‑null – factory throws NullPointerException
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_customFactoryThrowsNullPointerException() {
        String iri = "http://example.com/nullptr";
        // as per the case description
        SchemaValidatorsConfig cfg = null;
        reset(customFactoryMock);
        when(customFactoryMock.getMetaSchema(eq(iri), eq(factoryWithCustomFactory), eq(cfg))).thenThrow(new NullPointerException("null inside factory"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> invokeLoadMetaSchema(factoryWithCustomFactory, iri, cfg));
        assertEquals("null inside factory", ex.getMessage());
    }

    /* ----------------------------------------------------------------------
     * GROUP 6: metaSchemaFactory is null – fallback to DefaultJsonMetaSchemaFactory
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] iri={0}, config={1}")
    @MethodSource("defaultFactoryDelegateProvider")
    void loadMetaSchema_defaultFactoryDelegates(String iri, SchemaValidatorsConfig cfg, JsonMetaSchema expected) throws Exception {
        // mock the static singleton
        JsonMetaSchemaFactory defaultFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(defaultFactoryMock.getMetaSchema(eq(iri), eq(factoryWithNullFactory), eq(cfg))).thenReturn(expected);
        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance).thenReturn(defaultFactoryMock);
            JsonMetaSchema result = invokeLoadMetaSchema(factoryWithNullFactory, iri, cfg);
            assertSame(expected, result, "loadMetaSchema should return the JsonMetaSchema produced by the default factory");
            verify(defaultFactoryMock, times(1)).getMetaSchema(eq(iri), eq(factoryWithNullFactory), eq(cfg));
        }
    }

    /* ----------------------------------------------------------------------
     * GROUP 7: metaSchemaFactory is null – default factory returns null
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_defaultFactoryReturnsNull() throws Exception {
        String iri = "urn:jsonschema:meta-null";
        SchemaValidatorsConfig cfg = null;
        JsonMetaSchemaFactory defaultFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(defaultFactoryMock.getMetaSchema(eq(iri), eq(factoryWithNullFactory), eq(cfg))).thenReturn(null);
        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance).thenReturn(defaultFactoryMock);
            JsonMetaSchema result = invokeLoadMetaSchema(factoryWithNullFactory, iri, cfg);
            assertNull(result, "When the default factory returns null, loadMetaSchema must propagate null");
        }
    }

    /* ----------------------------------------------------------------------
     * GROUP 8: metaSchemaFactory is null – default factory throws IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_defaultFactoryThrowsIllegalArgumentException() {
        String iri = "unsupported:meta";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonMetaSchemaFactory defaultFactoryMock = mock(JsonMetaSchemaFactory.class);
        when(defaultFactoryMock.getMetaSchema(eq(iri), eq(factoryWithNullFactory), eq(cfg))).thenThrow(new IllegalArgumentException("unsupported IRI"));
        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance).thenReturn(defaultFactoryMock);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> invokeLoadMetaSchema(factoryWithNullFactory, iri, cfg));
            assertEquals("unsupported IRI", ex.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     * GROUP 9: metaSchemaFactory is null – DefaultJsonMetaSchemaFactory singleton is null (NPE)
     * ---------------------------------------------------------------------- */
    @Test
    void loadMetaSchema_defaultFactorySingletonIsNull() {
        String iri = "http://example.com/meta";
        SchemaValidatorsConfig cfg = null;
        try (MockedStatic<DefaultJsonMetaSchemaFactory> mockedStatic = mockStatic(DefaultJsonMetaSchemaFactory.class)) {
            // simulate getInstance() returning null
            mockedStatic.when(DefaultJsonMetaSchemaFactory::getInstance).thenReturn(null);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> invokeLoadMetaSchema(factoryWithNullFactory, iri, cfg));
            // The exact message is not defined; we just ensure NPE propagates.
            assertNotNull(ex);
        }
    }
}
