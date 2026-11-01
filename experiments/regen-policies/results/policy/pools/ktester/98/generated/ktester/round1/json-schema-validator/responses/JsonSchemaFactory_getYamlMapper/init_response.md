```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.YamlMapperFactory;

/**
 * Unit tests for {@link JsonSchemaFactory#getYamlMapper()}.
 *
 * <p>The tests cover the two possible execution paths:
 *
 * <ul>
 *   <li>When the factory was constructed with a non‑null {@code yamlMapper},
 *       {@code getYamlMapper()} must return that instance.
 *   <li>When the factory was constructed with a {@code null} {@code yamlMapper},
 *       {@code getYamlMapper()} must delegate to {@link YamlMapperFactory#getInstance()}.
 * </ul>
 *
 * <p>All required collaborators are mocked; the factory itself is instantiated via
 * reflection because its constructor is private.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getYamlMapper_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks shared by the tests                                            */
    /* --------------------------------------------------------------------- */
    @Mock JsonNodeReader jsonNodeReader;
    @Mock SchemaLoaders.Builder schemaLoadersBuilder;
    @Mock SchemaMappers.Builder schemaMappersBuilder;
    @Mock JsonMetaSchemaFactory metaSchemaFactory;

    private static final String DEFAULT_META_SCHEMA_IRI = "https://json-schema.org/draft/2020-12/schema";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required beyond the @Mock fields.
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
     * Helper that creates a {@link JsonSchemaFactory} instance via reflection.
     *
     * @param yamlMapper the mapper that should be stored inside the factory
     * @return a fully‑initialised factory
     * @throws Exception if reflection fails
     */
    private JsonSchemaFactory createFactory(ObjectMapper yamlMapper) throws Exception {
        // Minimal but valid arguments for the private constructor.
        ObjectMapper jsonMapper = new ObjectMapper(); // not used by the test
        Map<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<>();
        // The factory validates that the map contains the default meta‑schema IRI.
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_IRI),
                mock(JsonMetaSchema.class));

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class,
                        JsonNodeReader.class, String.class,
                        SchemaLoaders.Builder.class, SchemaMappers.Builder.class,
                        ConcurrentHashMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        return ctor.newInstance(
                jsonMapper,
                yamlMapper,
                jsonNodeReader,
                DEFAULT_META_SCHEMA_IRI,
                schemaLoadersBuilder,
                schemaMappersBuilder,
                new ConcurrentHashMap<>(metaSchemas),
                false,
                metaSchemaFactory);
    }

    @Test
    void getYamlMapper_returnsCustomMapperWhenSet() throws Exception {
        // GIVEN a factory that was constructed with a non‑null yaml mapper
        ObjectMapper customYamlMapper = new ObjectMapper();
        JsonSchemaFactory factory = createFactory(customYamlMapper);

        // WHEN the yaml mapper is requested
        ObjectMapper result = factory.getYamlMapper();

        // THEN the same instance that was supplied at construction is returned
        assertSame(customYamlMapper, result,
                "Factory should return the yaml mapper supplied at construction");
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
            assertSame(defaultMapper, result,
                    "Factory should delegate to YamlMapperFactory when its own yaml mapper is null");
        }
    }
}
```