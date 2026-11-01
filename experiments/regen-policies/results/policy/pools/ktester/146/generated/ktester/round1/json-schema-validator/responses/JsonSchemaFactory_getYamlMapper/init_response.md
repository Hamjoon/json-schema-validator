```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 * <p>The tests cover two scenarios:
 *
 * <ul>
 *   <li>When the factory was constructed with a non‑null {@code yamlMapper},
 *       the method must return that instance.</li>
 *   <li>When the factory was constructed with a {@code null} {@code yamlMapper},
 *       the method must delegate to {@link YamlMapperFactory#getInstance()}.</li>
 * </ul>
 *
 * <p>The factory constructor is private, therefore reflection is used to
 * instantiate it with the required arguments. All collaborators are mocked
 * because they are not relevant for the behaviour under test.
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
        // yamlMapper is deliberately null
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
}
```