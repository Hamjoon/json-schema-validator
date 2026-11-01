package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_2_Test {

    /**
     * Retrieves the {@code defaultMetaSchemaIri} value from a {@link JsonSchemaFactory}
     * instance using reflection.
     */
    private static String getDefaultMetaSchemaIri(JsonSchemaFactory factory) {
        try {
            Field f = JsonSchemaFactory.class.getDeclaredField("defaultMetaSchemaIri");
            f.setAccessible(true);
            return (String) f.get(factory);
        } catch (Exception e) {
            throw new AssertionError("Unable to access defaultMetaSchemaIri field via reflection", e);
        }
    }

    /**
     * Retrieves the {@code enableSchemaCache} flag from a {@link JsonSchemaFactory}
     * instance using reflection.
     */
    private static boolean isSchemaCacheEnabled(JsonSchemaFactory factory) {
        try {
            Field f = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
            f.setAccessible(true);
            return (boolean) f.get(factory);
        } catch (Exception e) {
            throw new AssertionError("Unable to access enableSchemaCache field via reflection", e);
        }
    }

    private static Stream<Arguments> versionFlagProvider() {
        return Stream.of(Arguments.of(SpecVersion.VersionFlag.V4), Arguments.of(SpecVersion.VersionFlag.V6), Arguments.of(SpecVersion.VersionFlag.V7), Arguments.of(SpecVersion.VersionFlag.V201909), Arguments.of(SpecVersion.VersionFlag.V202012));
    }

    /**
     * Verify that calling {@code getInstance} with a {@code null} customizer returns a
     * factory that contains the default meta‑schema for the supplied version flag.
     */
    @Test
    void getInstance_WithNullCustomizer_ReturnsFactoryWithDefaultMetaSchema() {
        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, null);
        // The default meta‑schema IRI for V202012 is defined inside the library.
        // We obtain it via the version enum to avoid hard‑coding the string.
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012);
        String defaultIri = version.getInstance().getIri();
        // Assert
        JsonMetaSchema meta = factory.getMetaSchema(defaultIri, SchemaValidatorsConfig.builder().build());
        assertNotNull(meta, "Factory should contain the default meta‑schema for the version flag");
        assertEquals(defaultIri, meta.getIri(), "Returned meta‑schema IRI should match the default");
    }

    /**
     * Verify that a customizer can replace the default meta‑schema IRI and supply a
     * custom {@link JsonMetaSchema}. The factory should then return the custom
     * meta‑schema when queried with the new IRI.
     */
    @Test
    void getInstance_WithCustomizer_ReplacesDefaultMetaSchema(@Mock JsonMetaSchema customMeta) {
        // Arrange: stub the IRI of the custom meta‑schema
        String customIri = "urn:custom:meta:schema";
        when(customMeta.getIri()).thenReturn(customIri);
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.defaultMetaSchemaIri(// set the new default IRI
        customIri).metaSchema(// register the custom meta‑schema
        customMeta);
        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, customizer);
        // Assert: the factory should return the mocked meta‑schema for the custom IRI
        JsonMetaSchema retrieved = factory.getMetaSchema(customIri, SchemaValidatorsConfig.builder().build());
        assertSame(customMeta, retrieved, "Factory should return the custom meta‑schema supplied via the customizer");
    }

    /**
     * Retrieves the internal {@code metaSchemas} map from a {@link JsonSchemaFactory}
     * instance using reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, JsonMetaSchema> getMetaSchemasMap(JsonSchemaFactory factory) {
        try {
            Field f = JsonSchemaFactory.class.getDeclaredField("metaSchemas");
            f.setAccessible(true);
            return (Map<String, JsonMetaSchema>) f.get(factory);
        } catch (Exception e) {
            throw new AssertionError("Unable to access metaSchemas field via reflection", e);
        }
    }

    @ParameterizedTest(name = "Version flag {0} without customizer")
    @MethodSource("versionFlagProvider")
    void getInstance_VersionFlag_NoCustomizer(SpecVersion.VersionFlag flag) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag, null);
        // Resolve expected default IRI via the version enum
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
        String expectedIri = version.getInstance().getIri();
        // Verify defaultMetaSchemaIri (reflection) and meta‑schema map size
        assertEquals(expectedIri, getDefaultMetaSchemaIri(factory), "defaultMetaSchemaIri should match the version‑derived IRI");
        Map<String, JsonMetaSchema> map = getMetaSchemasMap(factory);
        assertEquals(1, map.size(), "Meta‑schema map should contain exactly one entry");
        assertTrue(map.containsKey(expectedIri), "Map must contain the version‑derived meta‑schema");
    }

    @Test
    void getInstance_NullVersionFlag_ThrowsException() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(null, null), "Passing a null version flag should result in NPE from checkVersion");
    }

    @Test
    void getInstance_UnsupportedVersionFlag_ThrowsException() {
        // Assuming UNKNOWN is not a supported flag in the library
        assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.UNKNOWN, null), "Unsupported version flag should cause IllegalArgumentException");
    }

    @Test
    void getInstance_CustomizerOverridesDefaultIriAndCache() {
        // customizer changes default IRI and disables schema cache
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.defaultMetaSchemaIri("http://custom/meta").enableSchemaCache(false);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7, customizer);
        // default IRI should be the overridden one
        assertEquals("http://custom/meta", getDefaultMetaSchemaIri(factory));
        // cache flag should be false
        assertFalse(isSchemaCacheEnabled(factory), "Schema cache should be disabled by the customizer");
        // the original V7 meta‑schema must still be present
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7);
        String originalIri = version.getInstance().getIri();
        assertTrue(getMetaSchemasMap(factory).containsKey(originalIri), "Original version meta‑schema must still be registered");
    }

    @Test
    void getInstance_CustomizerAddsAdditionalMetaSchema(@Mock JsonMetaSchema extraMeta) {
        when(extraMeta.getIri()).thenReturn("http://example/custom");
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.metaSchema(extraMeta);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909, customizer);
        Map<String, JsonMetaSchema> map = getMetaSchemasMap(factory);
        assertEquals(2, map.size(), "Factory should contain both the version meta‑schema and the custom one");
        // verify both IRIs are present
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909);
        String versionIri = version.getInstance().getIri();
        assertTrue(map.containsKey(versionIri), "Version meta‑schema must be present");
        assertTrue(map.containsKey("http://example/custom"), "Custom meta‑schema must be present");
    }

    @Test
    void getInstance_CustomizerThrowsRuntimeException_Propagates() {
        Consumer<JsonSchemaFactory.Builder> throwingCustomizer = builder -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, throwingCustomizer));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void getInstance_NoOpCustomizer_ProducesSameFactoryAsNullCustomizer() {
        Consumer<JsonSchemaFactory.Builder> noop = builder -> {
            // deliberately empty
        };
        JsonSchemaFactory factoryWithNoop = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, noop);
        JsonSchemaFactory factoryWithNull = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, null);
        assertEquals(getDefaultMetaSchemaIri(factoryWithNull), getDefaultMetaSchemaIri(factoryWithNoop));
        assertEquals(getMetaSchemasMap(factoryWithNull).size(), getMetaSchemasMap(factoryWithNoop).size());
        assertTrue(getMetaSchemasMap(factoryWithNoop).keySet().containsAll(getMetaSchemasMap(factoryWithNull).keySet()));
    }

    @Test
    void getInstance_CustomizerModifiesSchemaLoaders_NoException() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.schemaLoaders(b -> {
            // add a dummy loader that does nothing (null return is acceptable for this test)
            b.addLoader(new SchemaLoader() {

                @Override
                public JsonSchema loadSchema(SchemaLocation location, SchemaValidatorsConfig cfg) {
                    return null;
                }
            });
        });
        // Should not throw any exception
        assertDoesNotThrow(() -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }

    @Test
    void getInstance_CustomizerAddsMultipleMetaSchemas_CombinedMapSizeThree() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.metaSchemas(map -> {
            JsonMetaSchema metaOne = new JsonMetaSchema("http://custom/one", JsonMetaSchema.getDefaultMetaSchema());
            JsonMetaSchema metaTwo = new JsonMetaSchema("http://custom/two", JsonMetaSchema.getDefaultMetaSchema());
            map.put(metaOne.getIri(), metaOne);
            map.put(metaTwo.getIri(), metaTwo);
        });
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer);
        Map<String, JsonMetaSchema> map = getMetaSchemasMap(factory);
        assertEquals(3, map.size(), "Factory should contain V4 meta‑schema + two custom meta‑schemas");
        assertTrue(map.containsKey("http://custom/one"));
        assertTrue(map.containsKey("http://custom/two"));
    }

    @Test
    void getInstance_CustomizerOverridesJsonMapper() {
        ObjectMapper customMapper = new ObjectMapper();
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.jsonMapper(customMapper);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer);
        assertSame(customMapper, factory.getJsonMapper(), "Factory should return the custom JSON ObjectMapper supplied via the builder");
    }

    @Test
    void getInstance_CustomizerOverridesYamlMapper() {
        ObjectMapper customYamlMapper = new ObjectMapper();
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.yamlMapper(customYamlMapper);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer);
        assertSame(customYamlMapper, factory.getYamlMapper(), "Factory should return the custom YAML ObjectMapper supplied via the builder");
    }

    @Test
    void getInstance_CustomizerSetsDefaultMetaSchemaIriToNull() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.defaultMetaSchemaIri(null);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer);
        assertNull(getDefaultMetaSchemaIri(factory), "defaultMetaSchemaIri should be null after customizer sets it");
        // still must contain the V4 meta‑schema
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4);
        assertTrue(getMetaSchemasMap(factory).containsKey(version.getInstance().getIri()));
    }

    @Test
    void getInstance_CustomizerMetaSchemaNull_ThrowsNPE() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.metaSchema(null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }

    @Test
    void getInstance_CustomizerMetaSchemasCollectionNull_ThrowsNPE() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.metaSchemas((Collection<? extends JsonMetaSchema>) null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }

    @Test
    void getInstance_CustomizerSchemaLoadersNull_ThrowsNPE() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.schemaLoaders(null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }

    @Test
    void getInstance_CustomizerSchemaMappersNull_ThrowsNPE() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.schemaMappers(null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }

    @Test
    void getInstance_CustomizerMetaSchemasConsumerNull_ThrowsNPE() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.metaSchemas((Consumer<Map<String, JsonMetaSchema>>) null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer));
    }
}
