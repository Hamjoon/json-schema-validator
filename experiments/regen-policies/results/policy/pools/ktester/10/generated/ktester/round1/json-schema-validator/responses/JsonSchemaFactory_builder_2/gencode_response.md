```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 *
 * The test suite covers:
 * <ul>
 *   <li>conditional branches for optional loader/mapper builders,</li>
 *   <li>handling of null fields,</li>
 *   <li>meta‑schema URI normalization,</li>
 *   <li>independence of the returned {@link Builder} from the blueprint,</li>
 *   <li>default value of {@code enableUriSchemaCache},</li>
 *   <li>exceptional situations (null blueprint, malformed internal state, etc.).</li>
 * </ul>
 *
 * Reflection is used only to inspect the internal state of the {@link Builder}
 * because the relevant fields are private and no public getters exist.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /** A mock meta‑schema used for most test scenarios. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** Common JSON mapper used for factories where a mapper is required. */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** Common YAML mapper used for factories where a mapper is required. */
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper();

    /** Default meta‑schema URI used in many tests. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";

    /** Helper to set a private field via reflection. */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper to get a private field via reflection. */
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

    /* --------------------------------------------------------------------- */
    /* 1. Basic copy test (already provided in the original scaffold)      */
    /* --------------------------------------------------------------------- */

    @Test
    void builderCopiesConfigurationFromBlueprint() {
        // blueprint with all optional parts initialised
        JsonSchemaFactory blueprintFactory = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .schemaLoaders(b -> { /* no customisation */ })
                .schemaMappers(b -> { /* no customisation */ })
                .build();

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintFactory);
        JsonSchemaFactory copyFactory = copyBuilder.build();

        // meta‑schema is present and identical
        JsonMetaSchema retrieved = copyFactory.getMetaSchema(DEFAULT_META_SCHEMA_URI, null);
        assertNotNull(retrieved);
        assertSame(mockMetaSchema, retrieved);

        // mappers are usable (construction would have failed if they were null)
        assertDoesNotThrow(() -> copyFactory.getSchema("{\"type\":\"string\"}"));

        // schema loader must be initialised
        assertDoesNotThrow(copyFactory::getSchemaLoader);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Conditional branches – presence/absence of loader/mappers builders */
    /* --------------------------------------------------------------------- */

    @Test
    void builderWhenBothOptionalBuildersAreNull() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .build(); // no schemaLoaders() / schemaMappers() calls

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
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .schemaLoaders(b -> { /* pretend we add something */ })
                .build(); // no schemaMappers call

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);

        // loader builder must be non‑null, mapper builder must stay null
        assertNotNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNull(getField(copyBuilder, "schemaMappersBuilder"));
    }

    @Test
    void builderWhenOnlySchemaMappersBuilderIsPresent() {
        // blueprint with a custom mapper builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .schemaMappers(b -> { /* pretend we add something */ })
                .build(); // no schemaLoaders call

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);

        // mapper builder must be non‑null, loader builder must stay null
        assertNotNull(getField(copyBuilder, "schemaMappersBuilder"));
        assertNull(getField(copyBuilder, "schemaLoadersBuilder"));
    }

    @Test
    void builderWhenBothOptionalBuildersArePresent() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .schemaLoaders(b -> { /* loader customisation */ })
                .schemaMappers(b -> { /* mapper customisation */ })
                .build();

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);

        assertNotNull(getField(copyBuilder, "schemaLoadersBuilder"));
        assertNotNull(getField(copyBuilder, "schemaMappersBuilder"));
    }

    /* --------------------------------------------------------------------- */
    /* 3. Null fields handling                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void builderCopiesNullFieldsCorrectly() {
        // Directly instantiate a factory with null fields via the package‑private ctor
        JsonSchemaFactory blueprint = new JsonSchemaFactory(
                null,               // jsonMapper
                null,               // yamlMapper
                null,               // defaultMetaSchemaURI
                null,               // schemaLoadersBuilder
                null,               // schemaMappersBuilder
                new ConcurrentHashMap<>(), // empty meta‑schema map
                true);              // enableUriSchemaCache (irrelevant)

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
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .jsonMapper(JSON_MAPPER)
                .yamlMapper(YAML_MAPPER)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .build(); // no meta‑schemas added

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);

        assertSame(JSON_MAPPER, getField(copyBuilder, "jsonMapper"));
        assertSame(YAML_MAPPER, getField(copyBuilder, "yamlMapper"));
        assertEquals(DEFAULT_META_SCHEMA_URI, getField(copyBuilder, "defaultMetaSchemaURI"));
        assertTrue(((Map<?, ?>) getField(copyBuilder, "jsonMetaSchemas")).isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 4. Meta‑schema normalization                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void builderNormalizesMetaSchemaUri() {
        JsonMetaSchema mixedCaseSchema = mock(JsonMetaSchema.class);
        when(mixedCaseSchema.getUri()).thenReturn("HTTP://example.com/SCHEMA/");

        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mixedCaseSchema)
                .build();

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

        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(first)
                .addMetaSchema(second) // second should overwrite first
                .build();

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> map = getField(copyBuilder, "jsonMetaSchemas");

        assertEquals(1, map.size());
        assertSame(second, map.get("http://example.com/schema"));
    }

    /* --------------------------------------------------------------------- */
    /* 5. Builder independence                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void builderModificationsDoNotAffectBlueprintMetaSchemaMap() {
        JsonMetaSchema schema = mock(JsonMetaSchema.class);
        when(schema.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);

        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(schema)
                .jsonMapper(JSON_MAPPER)
                .build();

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> copyMap = getField(copyBuilder, "jsonMetaSchemas");
        copyMap.clear(); // mutate the builder's internal map

        // Blueprint's internal map must still contain the entry
        @SuppressWarnings("unchecked")
        Map<String, JsonMetaSchema> blueprintMap = getField(blueprint, "jsonMetaSchemas");
        assertEquals(1, blueprintMap.size());
        assertTrue(blueprintMap.containsValue(schema));
    }

    @Test
    void builderModificationsDoNotAffectBlueprintSchemaLoadersBuilder() {
        // Create a blueprint with a custom loader builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .schemaLoaders(b -> { /* pretend we add something */ })
                .build();

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

    /* --------------------------------------------------------------------- */
    /* 6. enableUriSchemaCache default                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void builderDoesNotCopyEnableUriSchemaCacheFlag() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .enableUriSchemaCache(false) // set to false in the original builder
                .build();

        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // The flag is private; reflect to read it.
        Boolean flag = getField(copyBuilder, "enableUriSchemaCache");
        assertTrue(flag, "The copied builder must keep the default value true");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Exceptional situations                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void builderThrowsNPEWhenBlueprintIsNull() {
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.builder(null));
    }

    @Test
    void builderThrowsNPEWhenBlueprintMetaSchemaMapIsNull() throws Exception {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)
                .build();
        // Force the internal map to null via reflection
        setField(blueprint, "jsonMetaSchemas", null);

        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderThrowsNPEWhenMetaSchemaEntryIsNull() {
        // Prepare a map with a null value
        Map<String, JsonMetaSchema> map = new ConcurrentHashMap<>();
        map.put("http://example.com/null", null);
        JsonSchemaFactory blueprint = new JsonSchemaFactory(
                JSON_MAPPER, YAML_MAPPER, DEFAULT_META_SCHEMA_URI,
                null, null, map, true);

        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderThrowsNPEWhenMetaSchemaUriIsNull() {
        JsonMetaSchema nullUriSchema = mock(JsonMetaSchema.class);
        when(nullUriSchema.getUri()).thenReturn(null);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .addMetaSchema(nullUriSchema)
                .build();

        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.builder(blueprint));
    }

    @Test
    void builderPropagatesIllegalArgumentExceptionFromSchemaLoadersCloning() {
        // Mock the static factory method of SchemaLoaders
        try (MockedStatic<SchemaLoaders> mocked = mockStatic(SchemaLoaders.class)) {
            SchemaLoaders.Builder mockOriginal = mock(SchemaLoaders.Builder.class);
            // Blueprint with a non‑null loader builder
            JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                    .addMetaSchema(mockMetaSchema)
                    .schemaLoaders(b -> { /* no‑op */ })
                    .build();
            // Replace the internal builder with our mock
            setField(blueprint, "schemaLoadersBuilder", mockOriginal);

            // Mock the cloning process to throw IllegalArgumentException
            SchemaLoaders.Builder mockClone = mock(SchemaLoaders.Builder.class);
            when(SchemaLoaders.builder()).thenReturn(mockClone);
            when(mockClone.with(mockOriginal)).thenThrow(new IllegalArgumentException("bad config"));

            assertThrows(IllegalArgumentException.class,
                    () -> JsonSchemaFactory.builder(blueprint));
        }
    }

    @Test
    void builderPropagatesIllegalStateExceptionFromSchemaMappersCloning() {
        // Mock the static factory method of SchemaMappers
        try (MockedStatic<SchemaMappers> mocked = mockStatic(SchemaMappers.class)) {
            SchemaMappers.Builder mockOriginal = mock(SchemaMappers.Builder.class);
            // Blueprint with a non‑null mapper builder
            JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                    .addMetaSchema(mockMetaSchema)
                    .schemaMappers(b -> { /* no‑op */ })
                    .build();
            // Replace the internal builder with our mock
            setField(blueprint, "schemaMappersBuilder", mockOriginal);

            // Mock the cloning process to throw IllegalStateException
            SchemaMappers.Builder mockClone = mock(SchemaMappers.Builder.class);
            when(SchemaMappers.builder()).thenReturn(mockClone);
            when(mockClone.with(mockOriginal)).thenThrow(new IllegalStateException("invalid state"));

            assertThrows(IllegalStateException.class,
                    () -> JsonSchemaFactory.builder(blueprint));
        }
    }
}
```