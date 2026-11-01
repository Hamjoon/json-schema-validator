```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchemaFactory#createSchemaValidatorsConfig()}.
 *
 * The method is {@code protected} and simply returns a new {@link SchemaValidatorsConfig}
 * instance. The test verifies that:
 * <ul>
 *   <li>The returned object is not {@code null}.</li>
 *   <li>A fresh instance is created on each invocation (i.e. two calls do not return the same object).</li>
 * </ul>
 *
 * Because {@link JsonSchemaFactory} has a private constructor, the instance is created
 * via a Mockito spy that invokes the real constructor with suitable arguments.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createSchemaValidatorsConfig_Test {

    /** Instance under test – created as a spy so that real methods are executed. */
    private JsonSchemaFactory factory;

    /** Common test data. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal but valid arguments for the private constructor.
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectMapper yamlMapper = new ObjectMapper();

        // Builders can be instantiated directly – they have a public no‑arg constructor.
        SchemaLoaders.Builder schemaLoadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder schemaMappersBuilder = new SchemaMappers.Builder();

        // The factory validates that the map contains an entry for the default meta‑schema URI.
        // We mock {@link JsonMetaSchema} because its implementation is irrelevant for this test.
        JsonMetaSchema dummyMetaSchema = Mockito.mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        // The factory normalises the URI via a protected static method; in practice the
        // normalisation does not change the string for the standard URI, so we store it as‑is.
        jsonMetaSchemas.put(DEFAULT_META_SCHEMA_URI, dummyMetaSchema);

        // Build a real instance via Mockito's CALLS_REAL_METHODS setting.
        factory = Mockito.mock(
                JsonSchemaFactory.class,
                Mockito.withSettings()
                        .useConstructor(jsonMapper, yamlMapper,
                                DEFAULT_META_SCHEMA_URI,
                                schemaLoadersBuilder,
                                schemaMappersBuilder,
                                jsonMetaSchemas,
                                false)   // enableUriSchemaCache = false
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @AfterEach
    void teardownAfterEach() {
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void createSchemaValidatorsConfig_returnsNonNullInstance() {
        // When
        SchemaValidatorsConfig config = factory.createSchemaValidatorsConfig();

        // Then
        assertNotNull(config, "createSchemaValidatorsConfig() should never return null");
    }

    @Test
    void createSchemaValidatorsConfig_returnsNewInstanceOnEachCall() {
        // When
        SchemaValidatorsConfig first = factory.createSchemaValidatorsConfig();
        SchemaValidatorsConfig second = factory.createSchemaValidatorsConfig();

        // Then
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second,
                "Each call to createSchemaValidatorsConfig() should return a distinct instance");
    }
}
```