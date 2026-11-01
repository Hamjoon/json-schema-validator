package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.serialization.YamlMapperFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getYamlMapper_Test {

    /**
     * Common test data
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/draft-07/schema";

    /**
     * Mocks used in several tests
     */
    private ObjectMapper jsonMapperMock;

    private SchemaLoaders.Builder schemaLoadersBuilder;

    private SchemaMappers.Builder schemaMappersBuilder;

    private Map<String, JsonMetaSchema> metaSchemas;

    class MyObjectMapper extends ObjectMapper {
        // No additional behaviour needed for the test
    }

    class MyObjectMapper extends ObjectMapper {
        // No additional behaviour needed for the test
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh mocks / objects before each test
        jsonMapperMock = mock(ObjectMapper.class);
        schemaLoadersBuilder = new SchemaLoaders.Builder();
        schemaMappersBuilder = new SchemaMappers.Builder();
        // Build a minimal meta‑schema map required by the private constructor
        JsonMetaSchema metaSchemaMock = mock(JsonMetaSchema.class);
        metaSchemas = new HashMap<>();
        // The factory validates that the map contains an entry whose key equals the
        // normalized default meta‑schema URI. For the purpose of the test the
        // normalization does not change the string, so we can use the same key.
        metaSchemas.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);
    }

    /**
     * Helper that creates a {@link JsonSchemaFactory} instance using the private
     * constructor via reflection.
     *
     * @param yamlMapper the yaml mapper to pass (may be {@code null})
     * @return a fully‑initialised factory instance
     */
    private JsonSchemaFactory createFactory(ObjectMapper yamlMapper) throws Exception {
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, Map.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapperMock, yamlMapper, DEFAULT_META_SCHEMA_URI, schemaLoadersBuilder, schemaMappersBuilder, metaSchemas, false);
    }

    /**
     * Calls the protected {@code getYamlMapper()} method via reflection.
     *
     * @param factory the factory instance
     * @return the mapper returned by the method
     */
    private ObjectMapper invokeGetYamlMapper(JsonSchemaFactory factory) throws Exception {
        Method method = JsonSchemaFactory.class.getDeclaredMethod("getYamlMapper");
        method.setAccessible(true);
        return (ObjectMapper) method.invoke(factory);
    }

    @Test
    void whenYamlMapperIsProvided_thenGetYamlMapperReturnsIt() throws Exception {
        // Arrange – a non‑null yaml mapper
        ObjectMapper yamlMapperMock = mock(ObjectMapper.class);
        JsonSchemaFactory factory = createFactory(yamlMapperMock);
        // Act
        ObjectMapper result = invokeGetYamlMapper(factory);
        // Assert
        assertSame(yamlMapperMock, result, "getYamlMapper() should return the yaml mapper supplied to the constructor");
    }

    @Test
    void whenYamlMapperIsNull_thenGetYamlMapperReturnsFactoryDefault() throws Exception {
        // Arrange – yaml mapper is null, we expect the static factory to be used
        JsonSchemaFactory factory = createFactory(null);
        // Capture the instance that YamlMapperFactory would return
        ObjectMapper expected = YamlMapperFactory.getInstance();
        // Act
        ObjectMapper result = invokeGetYamlMapper(factory);
        // Assert
        assertNotNull(result, "Result must not be null when yamlMapper is null");
        assertSame(expected, result, "getYamlMapper() should delegate to YamlMapperFactory.getInstance() when yamlMapper is null");
    }

    @Test
    void whenYamlMapperIsNull_andYamlMapperFactoryIsMocked_thenGetYamlMapperUsesMock() throws Exception {
        // This test demonstrates that the static call can be mocked if desired.
        // Arrange
        JsonSchemaFactory factory = createFactory(null);
        ObjectMapper mockedMapper = mock(ObjectMapper.class);
        try (MockedStatic<YamlMapperFactory> mockedStatic = Mockito.mockStatic(YamlMapperFactory.class)) {
            mockedStatic.when(YamlMapperFactory::getInstance).thenReturn(mockedMapper);
            // Act
            ObjectMapper result = invokeGetYamlMapper(factory);
            // Assert
            assertSame(mockedMapper, result, "When YamlMapperFactory.getInstance() is mocked, getYamlMapper() should return the mock");
        }
    }

    @Test
    void whenInjectedYamlMapperEqualsDefault_thenTrueBranchIsTaken() throws Exception {
        // Arrange – obtain the default mapper and inject the exact same instance
        ObjectMapper defaultMapper = YamlMapperFactory.getInstance();
        JsonSchemaFactory factory = createFactory(defaultMapper);
        // Act
        ObjectMapper result = invokeGetYamlMapper(factory);
        // Assert – the returned reference must be the injected one, not a new instance
        assertSame(defaultMapper, result, "Even if the injected yamlMapper equals the default instance, the true branch should be taken and the same reference returned");
    }

    @Test
    void whenCustomObjectMapperSubclassIsInjected_thenItIsReturnedUnchanged() throws Exception {
        // Arrange – define a simple subclass of ObjectMapper
        class MyObjectMapper extends ObjectMapper {
            // No additional behaviour needed for the test
        }
        ObjectMapper customMapper = new MyObjectMapper();
        JsonSchemaFactory factory = createFactory(customMapper);
        // Act
        ObjectMapper result = invokeGetYamlMapper(factory);
        // Assert – the exact subclass instance must be returned
        assertSame(customMapper, result, "A custom ObjectMapper subclass injected into the factory must be returned unchanged");
        assertTrue(result instanceof MyObjectMapper, "Returned mapper should be an instance of the custom subclass");
    }

    @Test
    void whenYamlMapperIsSetLaterViaReflection_thenSubsequentCallsReflectChange() throws Exception {
        // Arrange – start with a null yamlMapper
        JsonSchemaFactory factory = createFactory(null);
        ObjectMapper firstCall = invokeGetYamlMapper(factory);
        ObjectMapper defaultMapper = YamlMapperFactory.getInstance();
        // Verify the first call falls back to the default mapper
        assertSame(defaultMapper, firstCall, "First call should return the default mapper from YamlMapperFactory");
        // Now inject a new mapper using reflection
        ObjectMapper newMapper = mock(ObjectMapper.class);
        Field yamlField = JsonSchemaFactory.class.getDeclaredField("yamlMapper");
        yamlField.setAccessible(true);
        yamlField.set(factory, newMapper);
        // Act – call the method again after the injection
        ObjectMapper secondCall = invokeGetYamlMapper(factory);
        // Assert – the newly injected mapper must be returned
        assertSame(newMapper, secondCall, "After setting the yamlMapper field via reflection, getYamlMapper() must return the new instance");
    }

    @Test
    void whenYamlMapperFactoryThrowsException_thenExceptionPropagates() throws Exception {
        // Arrange – factory with null yamlMapper; mock static to throw
        JsonSchemaFactory factory = createFactory(null);
        IllegalStateException expectedException = new IllegalStateException("factory broken");
        try (MockedStatic<YamlMapperFactory> mockedStatic = Mockito.mockStatic(YamlMapperFactory.class)) {
            mockedStatic.when(YamlMapperFactory::getInstance).thenThrow(expectedException);
            // Act & Assert – the exception should propagate unchanged
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> invokeGetYamlMapper(factory), "When YamlMapperFactory.getInstance() throws, getYamlMapper() must not catch it");
            assertSame(expectedException, thrown, "The thrown exception should be exactly the one produced by the mocked factory");
        }
    }
}
