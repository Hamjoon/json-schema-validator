package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.serialization.JsonNodeReader;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.mockito.MockedStatic;

class JsonSchemaFactory_builder_2_Test {

    private static final String CUSTOM_SCHEMA_ID = "https://example.com/custom-schema";

    private static final String CUSTOM_SCHEMA_JSON = "{\"type\":\"string\"}";

    private JsonSchemaFactory blueprint;

    private JsonSchemaFactory copyFactory;

    @BeforeEach
    void setupBeforeEach() {
        // Build a blueprint factory with a custom schema loader – used by a few
        // of the original tests.  Individual test methods create their own
        // blueprints when a different configuration is required.
        blueprint = JsonSchemaFactory.builder().schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap(CUSTOM_SCHEMA_ID, CUSTOM_SCHEMA_JSON))).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        copyFactory = copyBuilder.build();
    }

    /**
     * Returns the value of a private field via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void builderCopiesCoreComponents() {
        // Verify that the ObjectMapper instances are the same (they are package‑private getters).
        assertSame(blueprint.getJsonMapper(), copyFactory.getJsonMapper(), "JsonMapper should be copied by the builder");
        assertSame(blueprint.getYamlMapper(), copyFactory.getYamlMapper(), "YamlMapper should be copied by the builder");
        // Verify that the JsonNodeReader is copied.
        assertSame(blueprint.jsonNodeReader, copyFactory.jsonNodeReader, "JsonNodeReader should be copied by the builder");
    }

    @Test
    void builderCopiesMetaSchemas() {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // Default meta‑schema IRI for draft‑2020‑12 is known.
        String defaultIri = "https://json-schema.org/draft/2020-12/schema";
        JsonMetaSchema metaFromBlueprint = blueprint.getMetaSchema(defaultIri, config);
        JsonMetaSchema metaFromCopy = copyFactory.getMetaSchema(defaultIri, config);
        assertNotNull(metaFromBlueprint, "Blueprint must contain the default meta‑schema");
        assertNotNull(metaFromCopy, "Copy must contain the default meta‑schema");
        assertSame(metaFromBlueprint, metaFromCopy, "Meta‑schema instances should be identical after copying");
    }

    @Test
    void builderCopiesSchemaLoaders() {
        // The custom schema should be loadable from both factories.
        JsonSchema schemaFromBlueprint = blueprint.getSchema(CUSTOM_SCHEMA_ID);
        JsonSchema schemaFromCopy = copyFactory.getSchema(CUSTOM_SCHEMA_ID);
        assertNotNull(schemaFromBlueprint, "Blueprint must load the custom schema");
        assertNotNull(schemaFromCopy, "Copy must load the custom schema");
        // Validate a valid instance against the custom schema (a simple string).
        assertDoesNotThrow(() -> schemaFromBlueprint.validate("a string"));
        assertDoesNotThrow(() -> schemaFromCopy.validate("another string"));
    }

    /**
     * Casts the private field to the expected type (convenient wrapper).
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateFieldTyped(Object target, String fieldName, Class<T> type) throws Exception {
        return (T) getPrivateField(target, fieldName);
    }

    @Test
    void builderBothInternalBuildersNull() throws Exception {
        // Blueprint with no schemaLoadersBuilder and no schemaMappersBuilder.
        JsonSchemaFactory emptyBlueprint = JsonSchemaFactory.builder().build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(emptyBlueprint);
        // Using reflection to inspect the private fields of the Builder.
        SchemaLoaders.Builder loadersBuilder = getPrivateFieldTyped(copyBuilder, "schemaLoadersBuilder", SchemaLoaders.Builder.class);
        SchemaMappers.Builder mappersBuilder = getPrivateFieldTyped(copyBuilder, "schemaMappersBuilder", SchemaMappers.Builder.class);
        assertNull(loadersBuilder, "schemaLoadersBuilder should remain null");
        assertNull(mappersBuilder, "schemaMappersBuilder should remain null");
        // Verify simple fields are copied.
        assertSame(emptyBlueprint.getJsonMapper(), getPrivateFieldTyped(copyBuilder, "jsonMapper", ObjectMapper.class));
        assertSame(emptyBlueprint.getYamlMapper(), getPrivateFieldTyped(copyBuilder, "yamlMapper", ObjectMapper.class));
        assertSame(emptyBlueprint.jsonNodeReader, getPrivateFieldTyped(copyBuilder, "jsonNodeReader", JsonNodeReader.class));
        assertEquals(emptyBlueprint.defaultMetaSchemaIri, getPrivateFieldTyped(copyBuilder, "defaultMetaSchemaIri", String.class));
    }

    @Test
    void builderOnlySchemaLoadersBuilderNonNull() throws Exception {
        // Blueprint with a non‑null SchemaLoaders builder.
        JsonSchemaFactory blueprintWithLoaders = JsonSchemaFactory.builder().schemaLoaders(sl -> sl.schemas(Collections.singletonMap("urn:test", "{\"type\":\"integer\"}"))).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintWithLoaders);
        SchemaLoaders.Builder loadersBuilder = getPrivateFieldTyped(copyBuilder, "schemaLoadersBuilder", SchemaLoaders.Builder.class);
        SchemaMappers.Builder mappersBuilder = getPrivateFieldTyped(copyBuilder, "schemaMappersBuilder", SchemaMappers.Builder.class);
        assertNotNull(loadersBuilder, "schemaLoadersBuilder should be copied (non‑null)");
        assertNull(mappersBuilder, "schemaMappersBuilder should remain null");
        // Ensure the copied builder is a different instance from the blueprint's internal builder.
        Field blueprintLoadersField = JsonSchemaFactory.class.getDeclaredField("schemaLoadersBuilder");
        blueprintLoadersField.setAccessible(true);
        Object blueprintLoaders = blueprintLoadersField.get(blueprintWithLoaders);
        assertNotSame(blueprintLoaders, loadersBuilder, "The copied SchemaLoaders.Builder should be a new instance");
    }

    @Test
    void builderOnlySchemaMappersBuilderNonNull() throws Exception {
        // Blueprint with a non‑null SchemaMappers builder.
        JsonSchemaFactory blueprintWithMappers = JsonSchemaFactory.builder().schemaMappers(sm -> sm.mappers(Collections.singletonMap("custom", (node) -> node))).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintWithMappers);
        SchemaLoaders.Builder loadersBuilder = getPrivateFieldTyped(copyBuilder, "schemaLoadersBuilder", SchemaLoaders.Builder.class);
        SchemaMappers.Builder mappersBuilder = getPrivateFieldTyped(copyBuilder, "schemaMappersBuilder", SchemaMappers.Builder.class);
        assertNull(loadersBuilder, "schemaLoadersBuilder should remain null");
        assertNotNull(mappersBuilder, "schemaMappersBuilder should be copied (non‑null)");
        // Verify a new instance is created.
        Field blueprintMappersField = JsonSchemaFactory.class.getDeclaredField("schemaMappersBuilder");
        blueprintMappersField.setAccessible(true);
        Object blueprintMappers = blueprintMappersField.get(blueprintWithMappers);
        assertNotSame(blueprintMappers, mappersBuilder, "The copied SchemaMappers.Builder should be a new instance");
    }

    @Test
    void builderBothInternalBuildersNonNull() throws Exception {
        // Blueprint with both internal builders configured.
        JsonSchemaFactory blueprintBoth = JsonSchemaFactory.builder().schemaLoaders(sl -> sl.schemas(Collections.singletonMap("urn:test", "{\"type\":\"integer\"}"))).schemaMappers(sm -> sm.mappers(Collections.singletonMap("custom", (node) -> node))).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintBoth);
        SchemaLoaders.Builder loadersBuilder = getPrivateFieldTyped(copyBuilder, "schemaLoadersBuilder", SchemaLoaders.Builder.class);
        SchemaMappers.Builder mappersBuilder = getPrivateFieldTyped(copyBuilder, "schemaMappersBuilder", SchemaMappers.Builder.class);
        assertNotNull(loadersBuilder, "schemaLoadersBuilder should be copied");
        assertNotNull(mappersBuilder, "schemaMappersBuilder should be copied");
        // Ensure they are distinct objects from the blueprint's internal builders.
        Field blueprintLoadersField = JsonSchemaFactory.class.getDeclaredField("schemaLoadersBuilder");
        blueprintLoadersField.setAccessible(true);
        Object blueprintLoaders = blueprintLoadersField.get(blueprintBoth);
        assertNotSame(blueprintLoaders, loadersBuilder, "Copied SchemaLoaders.Builder must be a new instance");
        Field blueprintMappersField = JsonSchemaFactory.class.getDeclaredField("schemaMappersBuilder");
        blueprintMappersField.setAccessible(true);
        Object blueprintMappers = blueprintMappersField.get(blueprintBoth);
        assertNotSame(blueprintMappers, mappersBuilder, "Copied SchemaMappers.Builder must be a new instance");
    }

    @Test
    void builderCopiesNonEmptyMetaSchemasAndEnsuresImmutability() throws Exception {
        // Two mock JsonMetaSchema objects.
        JsonMetaSchema meta1 = mock(JsonMetaSchema.class);
        when(meta1.getIri()).thenReturn("http://example.com/meta1");
        JsonMetaSchema meta2 = mock(JsonMetaSchema.class);
        when(meta2.getIri()).thenReturn("http://example.com/meta2");
        JsonSchemaFactory blueprintWithMetas = JsonSchemaFactory.builder().metaSchema(meta1).metaSchema(meta2).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintWithMetas);
        // Access the private metaSchemas map of the Builder.
        ConcurrentMap<String, JsonMetaSchema> metaMap = getPrivateFieldTyped(copyBuilder, "metaSchemas", ConcurrentMap.class);
        assertEquals(2, metaMap.size(), "Meta‑schemas map should contain two entries");
        assertSame(meta1, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri(meta1.getIri())), "First meta‑schema instance should be identical");
        assertSame(meta2, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri(meta2.getIri())), "Second meta‑schema instance should be identical");
        // Mutate the copy's map and verify the blueprint's map is unaffected.
        metaMap.remove(JsonSchemaFactory.normalizeMetaSchemaUri(meta1.getIri()));
        assertNotNull(blueprintWithMetas.getMetaSchema(meta1.getIri(), SchemaValidatorsConfig.builder().build()), "Removing from the copy must not affect the original blueprint");
    }

    @Test
    void builderPreservesNullDefaultMetaSchemaIri() throws Exception {
        JsonSchemaFactory blueprintNullIri = JsonSchemaFactory.builder().defaultMetaSchemaIri(null).jsonMapper(new ObjectMapper()).yamlMapper(new ObjectMapper()).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintNullIri);
        String copiedIri = getPrivateFieldTyped(copyBuilder, "defaultMetaSchemaIri", String.class);
        assertNull(copiedIri, "defaultMetaSchemaIri should remain null in the copied builder");
    }

    @Test
    void builderCopiesJsonNodeReaderWhenPresent() throws Exception {
        JsonNodeReader reader = JsonNodeReader.builder().locationAware().build();
        JsonSchemaFactory blueprintWithReader = JsonSchemaFactory.builder().jsonNodeReader(reader).jsonMapper(new ObjectMapper()).yamlMapper(new ObjectMapper()).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintWithReader);
        JsonNodeReader copiedReader = getPrivateFieldTyped(copyBuilder, "jsonNodeReader", JsonNodeReader.class);
        assertSame(reader, copiedReader, "JsonNodeReader should be copied verbatim");
    }

    @Test
    void builderCopiesYamlMapperWhenJsonMapperIsNull() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper();
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().jsonMapper(// deprecated setter, but still usable.
        null).yamlMapper(yamlMapper).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        ObjectMapper copiedYaml = getPrivateFieldTyped(copyBuilder, "yamlMapper", ObjectMapper.class);
        ObjectMapper copiedJson = getPrivateFieldTyped(copyBuilder, "jsonMapper", ObjectMapper.class);
        assertSame(yamlMapper, copiedYaml, "yamlMapper should be copied");
        assertNull(copiedJson, "jsonMapper should remain null");
    }

    @Test
    void builderNormalizesMetaSchemaIriDuringCopy() throws Exception {
        // Mock a JsonMetaSchema whose IRI ends with a slash.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getIri()).thenReturn("http://example.com/meta/");
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().metaSchema(meta).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        ConcurrentMap<String, JsonMetaSchema> metaMap = getPrivateFieldTyped(copyBuilder, "metaSchemas", ConcurrentMap.class);
        assertTrue(metaMap.containsKey("http://example.com/meta"), "The key should be the normalized IRI without trailing slash");
        assertSame(meta, metaMap.get("http://example.com/meta"), "The associated JsonMetaSchema instance should be the same");
    }

    @Test
    void builderCopiesBothInternalBuildersWithCustomConfiguration() throws Exception {
        // Blueprint with custom loader and mapper.
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().schemaLoaders(sl -> sl.schemas(Collections.singletonMap("urn:custom", "{\"type\":\"boolean\"}"))).schemaMappers(sm -> sm.mappers(Collections.singletonMap("custom", (node) -> node))).build();
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprint);
        // Verify that the custom schema can be loaded from the copy.
        JsonSchemaFactory copyFactory = copyBuilder.build();
        JsonSchema schema = copyFactory.getSchema("urn:custom");
        assertNotNull(schema, "Custom schema should be loadable from the copied factory");
        assertDoesNotThrow(() -> schema.validate(true));
        // Ensure the internal builder objects are distinct.
        SchemaLoaders.Builder loadersBuilder = getPrivateFieldTyped(copyBuilder, "schemaLoadersBuilder", SchemaLoaders.Builder.class);
        SchemaMappers.Builder mappersBuilder = getPrivateFieldTyped(copyBuilder, "schemaMappersBuilder", SchemaMappers.Builder.class);
        assertNotNull(loadersBuilder);
        assertNotNull(mappersBuilder);
    }

    @Test
    void builderThrowsNullPointerExceptionWhenBlueprintIsNull() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(null), "Passing a null blueprint should result in NPE");
    }

    @Test
    void builderThrowsNullPointerExceptionWhenBlueprintMetaSchemasIsNull() throws Exception {
        // Build a normal blueprint then nullify its metaSchemas map via reflection.
        JsonSchemaFactory normal = JsonSchemaFactory.builder().build();
        Field metaSchemasField = JsonSchemaFactory.class.getDeclaredField("metaSchemas");
        metaSchemasField.setAccessible(true);
        // force null
        metaSchemasField.set(normal, null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(normal), "Null metaSchemas map should cause NPE");
    }

    @Test
    void builderThrowsNullPointerExceptionWhenMetaSchemasContainNullEntry() throws Exception {
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().build();
        // Insert a null entry into the internal metaSchemas map.
        Field metaSchemasField = JsonSchemaFactory.class.getDeclaredField("metaSchemas");
        metaSchemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> map = (ConcurrentMap<String, JsonMetaSchema>) metaSchemasField.get(blueprint);
        map.put("nullEntry", null);
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(blueprint), "Null entry in metaSchemas should cause NPE");
    }

    @Test
    void builderThrowsNullPointerExceptionWhenMetaSchemaHasNullIri() throws Exception {
        // Mock a JsonMetaSchema whose getIri() returns null.
        JsonMetaSchema badMeta = mock(JsonMetaSchema.class);
        when(badMeta.getIri()).thenReturn(null);
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().metaSchema(badMeta).build();
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder(blueprint), "MetaSchema with null IRI should cause NPE");
    }

    @Test
    void builderPropagatesExceptionFromFaultySchemaLoadersBuilder() throws Exception {
        // Mock a SchemaLoaders.Builder that throws when with(...) is called.
        SchemaLoaders.Builder faultyBuilder = mock(SchemaLoaders.Builder.class);
        when(faultyBuilder.with(any())).thenThrow(new IllegalArgumentException("faulty"));
        // Create a blueprint and inject the faulty builder via reflection.
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().build();
        Field loadersField = JsonSchemaFactory.class.getDeclaredField("schemaLoadersBuilder");
        loadersField.setAccessible(true);
        loadersField.set(blueprint, faultyBuilder);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.builder(blueprint), "Exception from faulty schemaLoadersBuilder should propagate");
        assertEquals("faulty", ex.getMessage());
    }

    @Test
    void builderPropagatesExceptionFromFaultySchemaMappersBuilder() throws Exception {
        // Mock a SchemaMappers.Builder that throws when with(...) is called.
        SchemaMappers.Builder faultyBuilder = mock(SchemaMappers.Builder.class);
        when(faultyBuilder.with(any())).thenThrow(new IllegalStateException("mapper broken"));
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().build();
        Field mappersField = JsonSchemaFactory.class.getDeclaredField("schemaMappersBuilder");
        mappersField.setAccessible(true);
        mappersField.set(blueprint, faultyBuilder);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> JsonSchemaFactory.builder(blueprint), "Exception from faulty schemaMappersBuilder should propagate");
        assertEquals("mapper broken", ex.getMessage());
    }
}
