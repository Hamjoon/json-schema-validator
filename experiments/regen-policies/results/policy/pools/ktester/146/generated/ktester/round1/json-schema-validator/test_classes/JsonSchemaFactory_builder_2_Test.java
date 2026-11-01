package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    private JsonSchemaFactory blueprint;

    private JsonMetaSchema metaSchema;

    private SchemaValidatorsConfig config;

    @BeforeEach
    void setupBeforeEach() {
        // use a known meta‑schema (draft‑2020‑12) as the default
        metaSchema = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V202012);
        config = SchemaValidatorsConfig.builder().build();
        // build a factory with custom meta‑schema, default IRI and a custom schema loader
        blueprint = JsonSchemaFactory.builder().metaSchema(// register the meta‑schema
        metaSchema).defaultMetaSchemaIri(// set it as default
        metaSchema.getIri()).schemaLoaders(sl -> sl.schemas(// custom schema loader entry
        Map.of("urn:test", "{\"type\":\"string\"}"))).build();
    }

    /**
     * Retrieves a private (or package‑private) field value via reflection.
     *
     * @param target    the object from which to read the field
     * @param fieldName the name of the field
     * @return the field value
     * @throws RuntimeException if reflection fails
     */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access field '" + fieldName + "' on " + target, e);
        }
    }

    /**
     * Sets a private (or package‑private) field via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     * @throws RuntimeException if reflection fails
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set field '" + fieldName + "' on " + target, e);
        }
    }

    /**
     * Verify that the static {@code builder(JsonSchemaFactory)} method copies
     * meta‑schemas and the default meta‑schema IRI from the blueprint.
     */
    @Test
    void builderCopiesMetaSchemasAndDefaultIri() {
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprint);
        JsonSchemaFactory copiedFactory = copiedBuilder.build();
        // The meta‑schema retrieved from the copied factory must be the same instance
        JsonMetaSchema originalMeta = blueprint.getMetaSchema(metaSchema.getIri(), config);
        JsonMetaSchema copiedMeta = copiedFactory.getMetaSchema(metaSchema.getIri(), config);
        assertSame(originalMeta, copiedMeta, "Meta schema should be identical after copying the builder");
        // The default meta‑schema IRI is implicitly verified by the above call;
        // if it were different, getMetaSchema would have thrown an exception.
    }

    /**
     * Verify that the schema loaders configured on the blueprint are also
     * present on a factory created from the copied builder.
     */
    @Test
    void builderCopiesSchemaLoaders() {
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprint);
        JsonSchemaFactory copiedFactory = copiedBuilder.build();
        // Both factories should be able to resolve the custom schema identified by "urn:test"
        JsonSchema originalSchema = blueprint.getSchema(URI.create("urn:test"));
        JsonSchema copiedSchema = copiedFactory.getSchema(URI.create("urn:test"));
        assertNotNull(originalSchema, "Original factory should resolve the custom schema");
        assertNotNull(copiedSchema, "Copied factory should resolve the custom schema");
        // The resolved schema nodes should be equal (both represent {"type":"string"})
        assertEquals(originalSchema.getSchemaNode().toString(), copiedSchema.getSchemaNode().toString(), "Schema nodes loaded by original and copied factories must be equal");
    }

    /**
     * Blueprint where both optional builders are {@code null}.
     * The returned Builder must keep those fields {@code null}.
     */
    @Test
    void builderLeavesOptionalBuildersNullWhenBlueprintHasNone() throws Exception {
        // blueprint already has no schemaLoadersBuilder / schemaMappersBuilder
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprint);
        Object schemaLoadersBuilder = getPrivateField(copiedBuilder, "schemaLoadersBuilder");
        Object schemaMappersBuilder = getPrivateField(copiedBuilder, "schemaMappersBuilder");
        assertNull(schemaLoadersBuilder, "schemaLoadersBuilder should be null");
        assertNull(schemaMappersBuilder, "schemaMappersBuilder should be null");
    }

    /**
     * Blueprint with non‑null {@code schemaLoadersBuilder} and {@code schemaMappersBuilder}.
     * The new Builder must contain *new* instances (deep copy) preserving configuration.
     */
    @Test
    void builderCreatesDeepCopiesOfNonNullOptionalBuilders() throws Exception {
        // create a blueprint with explicit (non‑null) builders
        JsonSchemaFactory blueprintWithBuilders = JsonSchemaFactory.builder().schemaLoaders(sl -> sl.schemas(Map.of("urn:custom", "{\"type\":\"integer\"}"))).schemaMappers(sm -> {
            // the consumer does not need to configure anything for the test;
            // it just forces the creation of a non‑null builder.
        }).build();
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprintWithBuilders);
        // obtain the private builder fields via reflection
        Object originalLoadersBuilder = getPrivateField(blueprintWithBuilders, "schemaLoadersBuilder");
        Object originalMappersBuilder = getPrivateField(blueprintWithBuilders, "schemaMappersBuilder");
        Object copiedLoadersBuilder = getPrivateField(copiedBuilder, "schemaLoadersBuilder");
        Object copiedMappersBuilder = getPrivateField(copiedBuilder, "schemaMappersBuilder");
        assertNotNull(originalLoadersBuilder, "original schemaLoadersBuilder must be non‑null");
        assertNotNull(originalMappersBuilder, "original schemaMappersBuilder must be non‑null");
        assertNotNull(copiedLoadersBuilder, "copied schemaLoadersBuilder must be non‑null");
        assertNotNull(copiedMappersBuilder, "copied schemaMappersBuilder must be non‑null");
        // they must be different instances (deep copy)
        assertNotSame(originalLoadersBuilder, copiedLoadersBuilder, "schemaLoadersBuilder should be a new instance, not the same reference");
        assertNotSame(originalMappersBuilder, copiedMappersBuilder, "schemaMappersBuilder should be a new instance, not the same reference");
    }

    /**
     * Blueprint with an empty meta‑schemas map and a {@code null} default IRI.
     * Builder should reflect the emptiness and allow building without error.
     */
    @Test
    void builderHandlesEmptyMetaSchemasAndNullDefaultIri() throws Exception {
        JsonSchemaFactory emptyBlueprint = JsonSchemaFactory.builder().// do NOT register any meta‑schema
        // do NOT set defaultMetaSchemaIri
        build();
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(emptyBlueprint);
        // metaSchemas map inside the Builder
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> metaMap = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(copiedBuilder, "metaSchemas");
        String defaultIri = (String) getPrivateField(copiedBuilder, "defaultMetaSchemaIri");
        assertTrue(metaMap.isEmpty(), "metaSchemas map must be empty");
        assertNull(defaultIri, "defaultMetaSchemaIri must be null");
        // building must not throw
        JsonSchemaFactory built = copiedBuilder.build();
        assertNotNull(built, "Factory should be built successfully");
    }

    /**
     * Blueprint containing two meta‑schemas whose IRIs differ only by a trailing '#'.
     * The builder must normalize IRIs and keep only the last supplied schema.
     */
    @Test
    void builderNormalizesMetaSchemaIriAndHandlesDuplicates() throws Exception {
        // mock meta‑schemas
        JsonMetaSchema metaA = mock(JsonMetaSchema.class);
        when(metaA.getIri()).thenReturn("http://example.com/meta#");
        JsonMetaSchema metaB = mock(JsonMetaSchema.class);
        when(metaB.getIri()).thenReturn("http://example.com/meta");
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder().metaSchema(// first entry (will be normalized to http://example.com/meta)
        metaA).metaSchema(// second entry with same normalized key, should overwrite
        metaB);
        JsonSchemaFactory factory = b.build();
        // normalized IRI should map to metaB
        JsonMetaSchema retrieved = factory.getMetaSchema("http://example.com/meta", config);
        assertSame(metaB, retrieved, "The second meta‑schema should overwrite the first after normalization");
        // internal map size must be 1
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> internalMap = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(b, "metaSchemas");
        assertEquals(1, internalMap.size(), "Only one normalized entry should remain");
    }

    /**
     * Blueprint where the deprecated mapper fields and the {@code JsonNodeReader}
     * are all {@code null}.  The resulting factory must preserve those nulls.
     */
    @Test
    void builderPreservesNullMapperAndReaderFields() throws Exception {
        JsonSchemaFactory nullMappersBlueprint = JsonSchemaFactory.builder().// explicitly set nulls (the builder methods accept null)
        jsonMapper(null).yamlMapper(null).jsonNodeReader(null).build();
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(nullMappersBlueprint);
        JsonSchemaFactory built = copiedBuilder.build();
        assertNull(built.getJsonMapper(), "jsonMapper should be null");
        assertNull(built.getYamlMapper(), "yamlMapper should be null");
        // jsonNodeReader is private; use reflection
        Object reader = getPrivateField(built, "jsonNodeReader");
        assertNull(reader, "jsonNodeReader should be null");
    }

    /**
     * Verify that the builder creates a defensive copy of the meta‑schemas map.
     * Mutating either the blueprint or the builder must not affect the other.
     */
    @Test
    void builderDefensiveCopyOfMetaSchemasMap() throws Exception {
        JsonMetaSchema dummy = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchemaFactory blueprintWithMap = JsonSchemaFactory.builder().metaSchema(dummy).defaultMetaSchemaIri(dummy.getIri()).build();
        // obtain blueprint's internal map
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> blueprintMap = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(blueprintWithMap, "metaSchemas");
        JsonSchemaFactory.Builder copiedBuilder = JsonSchemaFactory.builder(blueprintWithMap);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> builderMap = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(copiedBuilder, "metaSchemas");
        assertNotSame(blueprintMap, builderMap, "Builder must hold a separate copy of metaSchemas map");
        // mutate builder's map
        JsonMetaSchema extra = mock(JsonMetaSchema.class);
        when(extra.getIri()).thenReturn("http://example.com/extra");
        builderMap.put("http://example.com/extra", extra);
        assertFalse(blueprintMap.containsKey("http://example.com/extra"), "Changes to builder's map must not affect blueprint's map");
        // mutate blueprint's map
        JsonMetaSchema another = mock(JsonMetaSchema.class);
        when(another.getIri()).thenReturn("http://example.com/another");
        blueprintMap.put("http://example.com/another", another);
        assertFalse(builderMap.containsKey("http://example.com/another"), "Changes to blueprint's map must not affect builder's map");
    }

    /**
     * Passing {@code null} as the blueprint must result in a {@link NullPointerException}.
     */
    @Test
    void builderThrowsWhenBlueprintIsNull() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(null), "Calling builder(null) should throw NullPointerException");
    }

    /**
     * Blueprint whose internal {@code metaSchemas} map is {@code null}.
     * The builder method dereferences the map and must throw NPE.
     */
    @Test
    void builderThrowsWhenBlueprintMetaSchemasIsNull() throws Exception {
        JsonSchemaFactory normal = JsonSchemaFactory.builder().build();
        // force metaSchemas to null via reflection
        setPrivateField(normal, "metaSchemas", null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(normal), "Builder should throw NullPointerException when blueprint.metaSchemas is null");
    }

    /**
     * Blueprint with a {@code null} entry inside its meta‑schemas map.
     * The builder iterates over values and attempts to call {@code getIri()},
     * which must result in NPE.
     */
    @Test
    void builderThrowsWhenMetaSchemasContainsNullEntry() throws Exception {
        JsonSchemaFactory normal = JsonSchemaFactory.builder().build();
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> map = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(normal, "metaSchemas");
        // insert null value
        map.put("http://example.com/null", null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(normal), "Builder should throw NullPointerException when a meta‑schema entry is null");
    }

    /**
     * Blueprint with a meta‑schema whose {@code getIri()} returns {@code null}.
     * Normalization will receive a null argument and must throw NPE.
     */
    @Test
    void builderThrowsWhenMetaSchemaIriIsNull() throws Exception {
        JsonMetaSchema badMeta = mock(JsonMetaSchema.class);
        when(badMeta.getIri()).thenReturn(null);
        JsonSchemaFactory normal = JsonSchemaFactory.builder().build();
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> map = (ConcurrentMap<String, JsonMetaSchema>) getPrivateField(normal, "metaSchemas");
        map.put("null-iri-key", badMeta);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(normal), "Builder should throw NullPointerException when a meta‑schema IRI is null");
    }
}
