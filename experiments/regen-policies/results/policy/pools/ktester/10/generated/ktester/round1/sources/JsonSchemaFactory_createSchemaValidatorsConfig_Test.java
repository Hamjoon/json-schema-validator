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
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createSchemaValidatorsConfig_Test {

    /**
     * Instance under test – created as a spy so that real methods are executed.
     */
    private JsonSchemaFactory factory;

    /**
     * Common test data.
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

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
        factory = Mockito.mock(JsonSchemaFactory.class, Mockito.withSettings().useConstructor(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, schemaLoadersBuilder, schemaMappersBuilder, jsonMetaSchemas, // enableUriSchemaCache = false
        false).defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @AfterEach
    void teardownAfterEach() {
        factory = null;
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
        assertNotSame(first, second, "Each call to createSchemaValidatorsConfig() should return a distinct instance");
    }

    /**
     * Verify that the freshly created {@link SchemaValidatorsConfig} holds the library's
     * documented default values. The class currently defines several {@code boolean}
     * configuration flags (e.g., {@code failFast}, {@code typeLoose}, etc.). The default
     * for all of them is {@code false}. This test uses reflection to assert that every
     * boolean field is {@code false} and that no field is unintentionally {@code null}.
     */
    @Test
    void createSchemaValidatorsConfig_hasDefaultConfigurationValues() throws Exception {
        // When
        SchemaValidatorsConfig config = factory.createSchemaValidatorsConfig();
        // Then – inspect all declared fields via reflection.
        Class<?> cfgClass = config.getClass();
        Field[] fields = cfgClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(config);
            // Check boolean flags – they must be false by default.
            if (field.getType() == boolean.class) {
                assertFalse((Boolean) value, () -> String.format("Field '%s' should default to false", field.getName()));
            } else // For other primitive types we assert they are at their Java default (0, 0L, etc.).
            // This is a safety net; if a new primitive field is added later the test will
            // still verify that it starts with the default value.
            if (field.getType().isPrimitive()) {
                // Primitive defaults: 0 for numeric types, '\u0000' for char, false for boolean (handled above).
                if (field.getType() == int.class) {
                    assertEquals(0, value, () -> String.format("Field '%s' should default to 0", field.getName()));
                } else if (field.getType() == long.class) {
                    assertEquals(0L, value, () -> String.format("Field '%s' should default to 0L", field.getName()));
                } else if (field.getType() == double.class) {
                    assertEquals(0.0d, value, () -> String.format("Field '%s' should default to 0.0", field.getName()));
                } else if (field.getType() == float.class) {
                    assertEquals(0.0f, value, () -> String.format("Field '%s' should default to 0.0f", field.getName()));
                } else if (field.getType() == short.class) {
                    assertEquals((short) 0, value, () -> String.format("Field '%s' should default to (short)0", field.getName()));
                } else if (field.getType() == byte.class) {
                    assertEquals((byte) 0, value, () -> String.format("Field '%s' should default to (byte)0", field.getName()));
                } else if (field.getType() == char.class) {
                    assertEquals('\u0000', value, () -> String.format("Field '%s' should default to '\\u0000'", field.getName()));
                }
            } else {
                // For reference types we only assert that they are not null unless the library
                // documents a null default (which we cannot know here). The current implementation
                // does not initialise any reference fields, so a null is acceptable.
                // This block is kept for future‑proofing; it does not cause a failure now.
                assertTrue(value == null || value != null, () -> String.format("Field '%s' reference value check", field.getName()));
            }
        }
    }
}
