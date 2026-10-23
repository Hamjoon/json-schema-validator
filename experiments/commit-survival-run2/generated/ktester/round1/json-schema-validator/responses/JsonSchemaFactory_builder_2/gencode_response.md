```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests for {@link JsonSchemaFactory#builder(JsonSchemaFactory)} covering
 * the full matrix of scenarios described in the test‑case specification.
 *
 * <p>These tests rely on reflection to inspect the internal state of
 * {@link JsonSchemaFactory} because the builder does not expose a public API for
 * the copied configuration.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /** A mock {@link JsonMetaSchema} used as the sole meta‑schema for the blueprint. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** A mock {@link URIFactory} for a custom scheme. */
    @Mock
    private URIFactory mockUriFactory;

    /** A mock {@link URIFetcher} for the same custom scheme. */
    @Mock
    private URIFetcher mockUriFetcher;

    /** A mock {@link URITranslator} that could be added to the translator chain. */
    @Mock
    private URITranslator mockUriTranslator;

    /** A second mock {@link URITranslator} – useful for order‑checking tests. */
    @Mock
    private URITranslator mockUriTranslator2;

    /** A mock {@link URNFactory} – the builder intentionally discards this value. */
    @Mock
    private URNFactory mockUrnFactory;

    /** Blueprint factory that will be passed to {@code JsonSchemaFactory.builder(…) } */
    private JsonSchemaFactory blueprintFactory;

    /** Common constants used throughout the tests. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";
    private static final String CUSTOM_SCHEME = "custom";

    @BeforeEach
    void setUp() {
        // ----------------------------------------------------------------------
        // Build a fully‑featured blueprint that the first two tests rely on.
        // ----------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();

        // Populate the meta‑schema map with a single (mock) entry.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), mockMetaSchema);

        // Simple URI mapping – we keep a non‑empty map so that the copy operation can be verified.
        Map<String, String> uriMap = Collections.singletonMap("http", "https");

        blueprintFactory = JsonSchemaFactory.builder()
                .objectMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema)
                .addUriMappings(uriMap)
                .addUriTranslator(mockUriTranslator)
                .uriFactory(mockUriFactory, CUSTOM_SCHEME)
                .uriFetcher(mockUriFetcher, CUSTOM_SCHEME)
                .addUrnFactory(mockUrnFactory)
                .build();

        assertNotNull(blueprintFactory, "Blueprint factory should be created successfully");
    }

    @AfterEach
    void tearDown() {
        // No external resources to release.
    }

    // ----------------------------------------------------------------------
    // Helper methods for reflective access
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------------
    // Existing tests (unchanged)
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint data")
    void testBuilderCopiesBlueprintConfiguration() {
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        assertNotNull(derivedBuilder);
        assertNotSame(blueprintFactory, derivedBuilder);

        // Mutate the derived builder – the blueprint must stay functional.
        derivedBuilder.objectMapper(new ObjectMapper());
        JsonSchemaFactory mutatedFactory = derivedBuilder.build();

        assertDoesNotThrow(() -> blueprintFactory.getSchema("{\"type\":\"string\"}"));
        assertDoesNotThrow(() -> mutatedFactory.getSchema("{\"type\":\"string\"}"));
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) can be used to create a fully functional factory")
    void testBuilderProducesUsableFactory() {
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        JsonSchemaFactory newFactory = derivedBuilder.build();

        String schemaWithMeta = String.format("{\"$schema\":\"%s\",\"type\":\"string\"}", DEFAULT_META_SCHEMA_URI);
        JsonSchema schema = newFactory.getSchema(schemaWithMeta);
        assertNotNull(schema);
        assertDoesNotThrow(() -> schema.validate(new ObjectMapper().readTree("\"hello\"")));
    }

    // ----------------------------------------------------------------------
    // 1. Empty blueprint (all defaults)
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - handling of empty blueprint")
    void testBuilderWithEmptyBlueprint() {
        JsonSchemaFactory emptyBlueprint = JsonSchemaFactory.builder().build();

        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(emptyBlueprint);
        JsonSchemaFactory derivedFactory = derivedBuilder.build();

        // Verify that meta‑schema map is empty
        Map<String, JsonMetaSchema> metaMap = getPrivateField(derivedFactory, "jsonMetaSchemas");
        assertTrue(metaMap.isEmpty(), "Meta‑schema map should be empty for an empty blueprint");

        // defaultMetaSchemaURI must be null
        assertNull(getPrivateField(derivedFactory, "defaultMetaSchemaURI"));

        // ObjectMapper/YAMLMapper must be the SAME instances as in the empty blueprint
        assertSame(getPrivateField(emptyBlueprint, "jsonMapper"),
                getPrivateField(derivedFactory, "jsonMapper"),
                "ObjectMapper instance should be copied by reference");
        assertSame(getPrivateField(emptyBlueprint, "yamlMapper"),
                getPrivateField(derivedFactory, "yamlMapper"),
                "YAMLMapper instance should be copied by reference");

        // uriMap must be empty
        Map<String, String> uriMap = getPrivateField(derivedFactory, "uriMap");
        assertTrue(uriMap.isEmpty(), "URI map should be empty");

        // Translators list must be empty (apart from the default CompositeURITranslator internals)
        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> innerList = getPrivateField(translator, "translators");
        assertTrue(innerList.isEmpty(), "No custom URITranslators should be present");
    }

    // ----------------------------------------------------------------------
    // 2. Copies defaultMetaSchemaURI
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies defaultMetaSchemaURI")
    void testBuilderCopiesDefaultMetaSchemaURI() {
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("https://example.com/meta")
                .build();

        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(customFactory);
        JsonSchemaFactory derivedFactory = derivedBuilder.build();

        String copiedUri = getPrivateField(derivedFactory, "defaultMetaSchemaURI");
        assertEquals("https://example.com/meta", copiedUri);
    }

    // ----------------------------------------------------------------------
    // 3. Copies custom ObjectMapper and YAMLMapper instances
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom ObjectMapper and YAMLMapper instances")
    void testBuilderCopiesCustomMappers() {
        ObjectMapper customMapper = new ObjectMapper();
        YAMLMapper customYaml = new YAMLMapper();

        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .objectMapper(customMapper)
                .yamlMapper(customYaml)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(customFactory).build();

        assertSame(customMapper, getPrivateField(derivedFactory, "jsonMapper"));
        assertSame(customYaml, getPrivateField(derivedFactory, "yamlMapper"));
    }

    // ----------------------------------------------------------------------
    // 4. Copies added meta‑schemas
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies added meta‑schemas")
    void testBuilderCopiesMetaSchemas() {
        JsonMetaSchema meta1 = mock(JsonMetaSchema.class);
        JsonMetaSchema meta2 = mock(JsonMetaSchema.class);
        when(meta1.getUri()).thenReturn("urn:meta:1");
        when(meta2.getUri()).thenReturn("urn:meta:2");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .addMetaSchema(meta1)
                .addMetaSchema(meta2)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        Map<String, JsonMetaSchema> metaMap = getPrivateField(derivedFactory, "jsonMetaSchemas");
        assertEquals(2, metaMap.size());
        assertSame(meta1, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri("urn:meta:1")));
        assertSame(meta2, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri("urn:meta:2")));
    }

    // ----------------------------------------------------------------------
    // 5. Copies deprecated URI mappings (uriMap)
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies deprecated URI mappings (uriMap)")
    void testBuilderCopiesUriMappings() {
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com", "http://new.com");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .addUriMappings(map)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        Map<String, String> copiedMap = getPrivateField(derivedFactory, "uriMap");
        assertEquals(map, copiedMap);
    }

    // ----------------------------------------------------------------------
    // 6. Copies custom URIFactory mappings
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFactory mappings")
    void testBuilderCopiesUriFactoryMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .uriFactory(mockUriFactory, CUSTOM_SCHEME)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        URISchemeFactory schemeFactory = getPrivateField(derivedFactory, "uriFactory");
        // The factory provides a public accessor for its internal map via reflection.
        Map<String, URIFactory> map = getPrivateField(schemeFactory, "uriFactories");
        assertEquals(mockUriFactory, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    // 7. Copies custom URIFetcher mappings
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFetcher mappings")
    void testBuilderCopiesUriFetcherMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .uriFetcher(mockUriFetcher, CUSTOM_SCHEME)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        URISchemeFetcher schemeFetcher = getPrivateField(derivedFactory, "uriFetcher");
        Map<String, URIFetcher> map = getPrivateField(schemeFetcher, "uriFetchers");
        assertEquals(mockUriFetcher, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    // 8. Copies URITranslators (order preserved)
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies URITranslators")
    void testBuilderCopiesUriTranslators() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .addUriTranslator(mockUriTranslator)
                .addUriTranslator(mockUriTranslator2)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> list = getPrivateField(translator, "translators");
        assertEquals(2, list.size());
        assertSame(mockUriTranslator, list.get(0));
        assertSame(mockUriTranslator2, list.get(1));
    }

    // ----------------------------------------------------------------------
    // 9. does NOT copy urnFactory or enableUriSchemaCache
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - does NOT copy urnFactory or enableUriSchemaCache")
    void testBuilderIgnoresUrnFactoryAndCacheFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .addUrnFactory(mockUrnFactory)
                .enableUriSchemaCache(false)
                .build();

        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();

        // urnFactory should be null (default)
        assertNull(getPrivateField(derivedFactory, "urnFactory"));

        // enableUriSchemaCache should be true (builder's default)
        Boolean cacheFlag = get