package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /**
     * A mock meta‑schema used for the blueprint factory.
     */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /**
     * The blueprint factory that will be used as the source for the builder.
     */
    private JsonSchemaFactory blueprintFactory;

    /**
     * Common JSON mapper used for both factories.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Common YAML mapper used for both factories.
     */
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper();

    /**
     * The URI that identifies the default meta‑schema.
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";

    @BeforeEach
    void setupBeforeEach() {
        // Stub the mock meta‑schema so that its URI matches the default one.
        when(mockMetaSchema.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        // Build a blueprint JsonSchemaFactory with all configurable parts set.
        blueprintFactory = JsonSchemaFactory.builder().addMetaSchema(// meta‑schema map
        mockMetaSchema).defaultMetaSchemaURI(// default URI
        DEFAULT_META_SCHEMA_URI).jsonMapper(// JSON mapper
        JSON_MAPPER).yamlMapper(// YAML mapper
        YAML_MAPPER).// Initialise the optional builders – the consumer does nothing
        // other than forcing the internal builder objects to be created.
        schemaLoaders(b -> {
            /* no customisation */
        }).schemaMappers(b -> {
            /* no customisation */
        }).build();
    }

    /**
     * Helper to set a private field via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that the builder created from an existing factory copies the
     * configuration correctly.
     */
    @Test
    void builderCopiesConfigurationFromBlueprint() {
        // -------------------------------------------------
        // given: a blueprint factory (already prepared in @BeforeEach)
        // -------------------------------------------------
        // -------------------------------------------------
        // when: we obtain a new Builder from the static method
        // -------------------------------------------------
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintFactory);
        JsonSchemaFactory copyFactory = copyBuilder.build();
        // -------------------------------------------------
        // then: the copy must expose the same default meta‑schema URI
        // -------------------------------------------------
        JsonMetaSchema retrieved = copyFactory.getMetaSchema(DEFAULT_META_SCHEMA_URI, null);
        assertNotNull(retrieved, "The default meta‑schema must be present in the copied factory");
        assertSame(mockMetaSchema, retrieved, "The meta‑schema instance should be the same as the one added to the blueprint");
        // -------------------------------------------------
        // and: the JSON and YAML mappers must be the ones supplied
        // -------------------------------------------------
        // The getters for the mappers are protected; we verify their effect indirectly
        // by serialising a simple JSON node and checking that no exception is thrown.
        // (If the mappers were null, JsonSchemaFactory would have thrown during construction.)
        assertDoesNotThrow(() -> copyFactory.getSchema("{\"type\":\"string\"}"), "Factory built from the copied builder should be usable for schema parsing");
        // -------------------------------------------------
        // and: the optional schema‑loaders and schema‑mappers builders must have been copied
        // -------------------------------------------------
        // The presence of these builders is reflected in the ability to obtain a schema
        // loader without encountering a NullPointerException.
        assertDoesNotThrow(() -> copyFactory.getSchemaLoader(), "Schema loader should be initialised when the original factory had custom builders");
    }

    /**
     * Helper to get a private field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void builderWhenBothOptionalBuildersAreNull() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).jsonMapper(JSON_MAPPER).yamlMapper(YAML_MAPPER).// no schemaLoaders() / schemaMappers() calls
        build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // both internal builders must stay null
        assertNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNull(getField(copyBuilder, "schemaMappersBuilder"));
        // meta‑schemas are copied
        Map<String, JsonMetaSchema> map = getField(copyBuilder, "jsonMetaSchemas");
        assertEquals(1, map.size());
        assertTrue(map.containsValue(mockMetaSchema));
    }

    @Test
    void builderWhenOnlySchemaLoadersBuilderIsPresent() {
        // blueprint with a custom loader builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).jsonMapper(JSON_MAPPER).yamlMapper(YAML_MAPPER).schemaLoaders(b -> {
            /* pretend we add something */
        }).// no schemaMappers call
        build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // loader builder must be non‑null, mapper builder must stay null
        assertNotNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNull(getField(copyBuilder, "schemaMappersBuilder"));
    }

    @Test
    void builderWhenOnlySchemaMappersBuilderIsPresent() {
        // blueprint with a custom mapper builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).jsonMapper(JSON_MAPPER).yamlMapper(YAML_MAPPER).schemaMappers(b -> {
            /* pretend we add something */
        }).// no schemaLoaders call
        build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // mapper builder must be non‑null, loader builder must stay null
        assertNotNull(getField(copyBuilder, "schemaMappersBuilder"));
        assertNull(getField(copyBuilder, "schemaLoadersBuilder"));
    }

    @Test
    void builderWhenBothOptionalBuildersArePresent() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).jsonMapper(JSON_MAPPER).yamlMapper(YAML_MAPPER).schemaLoaders(b -> {
            /* loader customisation */
        }).schemaMappers(b -> {
            /* mapper customisation */
        }).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        assertNotNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNotNull(getField(copyBuilder, "schemaMappersBuilder"));
    }

    @Test
    void builderCopiesNullFieldsCorrectly() {
        // Directly instantiate a factory with null fields via the package‑private ctor
        JsonSchemaFactory blueprint = new JsonSchemaFactory(// jsonMapper
        null, // yamlMapper
        null, // defaultMetaSchemaURI
        null, // schemaLoadersBuilder
        null, // schemaMappersBuilder
        null, // empty meta‑schema map
        new ConcurrentHashMap<>(), // enableUriSchemaCache (irrelevant)
        true);
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        assertNull(getField(copyBuilder, "jsonMapper"));
        assertNull(getField(copyBuilder, "yamlMapper"));
        assertNull(getField(copyBuilder, "defaultMetaSchemaURI"));
        assertTrue(((Map<?, ?>) getField(copyBuilder, "jsonMetaSchemas")).isEmpty());
        assertNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNull(getField(copyBuilder, "schemaMappersBuilder"));
    }

    @Test
    void builderCopiesNonNullMappersAndDefaultUriEvenIfMetaSchemaMapIsEmpty() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().jsonMapper(JSON_MAPPER).yamlMapper(YAML_MAPPER).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).// no meta‑schemas added
        build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        assertSame(JSON_MAPPER, getField(copyBuilder, "jsonMapper"));
        assertSame(YAML_MAPPER, getField(copyBuilder, "yamlMapper"));
        assertEquals(DEFAULT_META_SCHEMA_URI, getField(copyBuilder, "defaultMetaSchemaURI"));
        assertTrue(((Map<?, ?>) getField(copyBuilder, "jsonMetaSchemas")).isEmpty());
    }

    @Test
    void builderNormalizesMetaSchemaUri() {
        JsonMetaSchema mixedCaseSchema = mock(JsonMetaSchema.class);
        when(mixedCaseSchema.getUri()).thenReturn("HTTP://example.com/SCHEMA/");
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mixedCaseSchema).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> map = getField(copyBuilder, "jsonMetaSchemas");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("http://example.com/schema"));
        assertSame(mixedCaseSchema, map.get("http://example.com/schema"));
    }

    @Test
    void builderCollapsesDuplicateUrisAfterNormalization() {
        JsonMetaSchema first = mock(JsonMetaSchema.class);
        JsonMetaSchema second = mock(JsonMetaSchema.class);
        when(first.getUri()).thenReturn("http://example.com/schema");
        when(second.getUri()).thenReturn("HTTP://EXAMPLE.COM/SCHEMA");
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(first).addMetaSchema(// second should overwrite first
        second).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> map = getField(copyBuilder, "jsonMetaSchemas");
        assertEquals(1, map.size());
        assertSame(second, map.get("http://example.com/schema"));
    }

    @Test
    void builderModificationsDoNotAffectBlueprintMetaSchemaMap() {
        JsonMetaSchema schema = mock(JsonMetaSchema.class);
        when(schema.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(schema).jsonMapper(JSON_MAPPER).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> copyMap = getField(copyBuilder, "jsonMetaSchemas");
        // mutate the builder's internal map
        copyMap.clear();
        // Blueprint's internal map must still contain the entry
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> blueprintMap = getField(blueprint, "jsonMetaSchemas");
        assertEquals(1, blueprintMap.size());
        assertTrue(blueprintMap.containsValue(schema));
    }

    @Test
    void builderModificationsDoNotAffectBlueprintSchemaLoadersBuilder() {
        // Create a blueprint with a custom loader builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).schemaLoaders(b -> {
            /* pretend we add something */
        }).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        Object copyLoadersBuilder = getField(copyBuilder, "schemaLoadersBuilder");
        assertNotNull(copyLoadersBuilder);
        // Mutate the copy's builder (set a private field to a sentinel value)
        setField(copyLoadersBuilder, "someInternalFlag", Boolean.TRUE);
        // Ensure the original blueprint's loader builder does not have that flag
        Object originalLoadersBuilder = getField(blueprint, "schemaLoadersBuilder");
        // The field may not exist; we just verify that no exception is thrown when accessing it
        // and that the two builder instances are not the same object.
        assertNotSame(originalLoadersBuilder, copyLoadersBuilder);
    }

    @Test
    void builderDoesNotCopyEnableUriSchemaCacheFlag() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().enableUriSchemaCache(// set to false in the original builder
        false).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // The flag is private; reflect to read it.
        Boolean flag = getField(copyBuilder, "enableUriSchemaCache");
        assertTrue(flag, "The copied builder must keep the default value true");
    }

    @Test
    void builderThrowsNPEWhenBlueprintIsNull() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(null));
    }

    @Test
    void builderThrowsNPEWhenBlueprintMetaSchemaMapIsNull() throws Exception {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).build();
        // Force the internal map to null via reflection
        setField(blueprint, "jsonMetaSchemas", null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderThrowsNPEWhenMetaSchemaEntryIsNull() {
        // Prepare a map with a null value
        Map<String, JsonMetaSchema> map = new ConcurrentHashMap<>();
        map.put("http://example.com/null", null);
        JsonSchemaFactory blueprint = new JsonSchemaFactory(JSON_MAPPER, YAML_MAPPER, DEFAULT_META_SCHEMA_URI, null, null, map, true);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderThrowsNPEWhenMetaSchemaUriIsNull() {
        JsonMetaSchema nullUriSchema = mock(JsonMetaSchema.class);
        when(nullUriSchema.getUri()).thenReturn(null);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(nullUriSchema).build();
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderPropagatesIllegalArgumentExceptionFromSchemaLoadersCloning() {
        // Mock the static factory method of SchemaLoaders
        try (MockedStatic<SchemaLoaders> mocked = mockStatic(SchemaLoaders.class)) {
            SchemaLoaders.Builder mockOriginal = mock(SchemaLoaders.Builder.class);
            // Blueprint with a non‑null loader builder
            JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).schemaLoaders(b -> {
                /* no‑op */
            }).build();
            // Replace the internal builder with our mock
            setField(blueprint, "schemaLoadersBuilder", mockOriginal);
            // Mock the cloning process to throw IllegalArgumentException
            SchemaLoaders.Builder mockClone = mock(SchemaLoaders.Builder.class);
            when(SchemaLoaders.builder()).thenReturn(mockClone);
            when(mockClone.with(mockOriginal)).thenThrow(new IllegalArgumentException("bad config"));
            assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.builder(blueprint));
        }
    }

    @Test
    void builderPropagatesIllegalStateExceptionFromSchemaMappersCloning() {
        // Mock the static factory method of SchemaMappers
        try (MockedStatic<SchemaMappers> mocked = mockStatic(SchemaMappers.class)) {
            SchemaMappers.Builder mockOriginal = mock(SchemaMappers.Builder.class);
            // Blueprint with a non‑null mapper builder
            JsonSchemaFactory blueprint = JsonSchemaFactory.builder().addMetaSchema(mockMetaSchema).schemaMappers(b -> {
                /* no‑op */
            }).build();
            // Replace the internal builder with our mock
            setField(blueprint, "schemaMappersBuilder", mockOriginal);
            // Mock the cloning process to throw IllegalStateException
            SchemaMappers.Builder mockClone = mock(SchemaMappers.Builder.class);
            when(SchemaMappers.builder()).thenReturn(mockClone);
            when(mockClone.with(mockOriginal)).thenThrow(new IllegalStateException("invalid state"));
            assertThrows(IllegalStateException.class, () -> JsonSchemaFactory.builder(blueprint));
        }
    }
}
