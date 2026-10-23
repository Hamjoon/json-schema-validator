```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

class JsonSchemaFactory_builder_2_Test {

    /**
     * Blueprint factory that will be passed to {@code JsonSchemaFactory.builder(…) }
     */
    private JsonSchemaFactory blueprintFactory;

    /**
     * Common constants used throughout the tests.
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";

    private static final String CUSTOM_SCHEME = "custom";

    /**
     * A second mock {@link URITranslator} – useful for order‑checking tests.
     * (Here we just use a dummy implementation; no Mockito needed.)
     */
    private final DummyURITranslator dummyUriTranslator = new DummyURITranslator();
    private final DummyURITranslator dummyUriTranslator2 = new DummyURITranslator();

    private final DummyURIFactory dummyUriFactory = new DummyURIFactory();
    private final DummyURIFetcher dummyUriFetcher = new DummyURIFetcher();
    private final DummyURNFactory dummyUrnFactory = new DummyURNFactory();

    private static class DummyURIFactory implements URIFactory {

        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        // Some versions of the library declare a two‑argument method.
        // We keep it without @Override to stay compatible with both versions.
        public URI create(URI base, String uri) {
            // Resolve against the base if provided, otherwise treat as absolute.
            return (base == null) ? URI.create(uri) : base.resolve(uri);
        }
    }

    private static class DummyURIFetcher implements URIFetcher {

        @Override
        public java.io.InputStream fetch(URI uri) {
            // not used in the tests
            return null;
        }
    }

    private static class DummyURITranslator implements URITranslator {

        @Override
        public URI translate(URI uri) {
            // identity translator
            return uri;
        }
    }

    private static class DummyURNFactory implements URNFactory {

        @Override
        public URI create(String urn) {
            return URI.create(urn);
        }
    }

    /**
     * A very small {@link JsonMetaSchema} implementation that only provides a
     * URI. All other methods throw {@link UnsupportedOperationException} because
     * they are never exercised by the tests.
     */
    private static class SimpleMetaSchema extends JsonMetaSchema {

        private final String uri;

        SimpleMetaSchema(String uri) {
            // JsonMetaSchema constructor: (String id, String uri, Map<String, Keyword> keywords)
            super(uri, uri, Collections.emptyMap());
            this.uri = uri;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public JsonSchemaFactory getFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonMetaSchema.Builder getBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setUp() {
        // ------------------------------------------------------------------
        // 1. Prepare a minimal but complete set of constructor arguments.
        // ------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();

        // The meta‑schema map must contain an entry for the default meta‑schema URI.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI),
                new SimpleMetaSchema(DEFAULT_META_SCHEMA_URI));

        // Simple URI mapping – can be empty, but we add one entry to make the test
        // observable.
        Map<String, String> uriMap = Collections.singletonMap("http", "https");

        // Build the blueprint factory using the public Builder API.
        blueprintFactory = JsonSchemaFactory.builder()
                .objectMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .addMetaSchema(new SimpleMetaSchema(DEFAULT_META_SCHEMA_URI))
                .addUriMappings(uriMap)
                .addUriTranslator(dummyUriTranslator)
                .uriFactory(dummyUriFactory, CUSTOM_SCHEME)
                .uriFetcher(dummyUriFetcher, CUSTOM_SCHEME)
                .addUrnFactory(dummyUrnFactory)
                .build();

        // Sanity check – the blueprint must be usable; any exception here would
        // indicate a problem with the test setup rather than the method under test.
        assertNotNull(blueprintFactory, "Blueprint factory should be created successfully");
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

    @Test
    @DisplayName("builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint data")
    void testBuilderCopiesBlueprintConfiguration() {
        // WHEN: we request a builder based on the blueprint factory
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // THEN: the returned builder must be a distinct, non‑null instance
        assertNotNull(derivedBuilder, "The derived builder must not be null");
        assertNotSame(blueprintFactory, derivedBuilder, "The derived builder must not be the same instance as the blueprint factory");
        // AND: mutating the derived builder must not affect the original blueprint
        // a different mapper instance
        ObjectMapper newMapper = new ObjectMapper();
        derivedBuilder.objectMapper(newMapper);
        // Build a new factory from the mutated builder.
        JsonSchemaFactory mutatedFactory = derivedBuilder.build();
        // The blueprint's ObjectMapper should remain unchanged.
        assertDoesNotThrow(() -> blueprintFactory.getSchema("{\"type\":\"string\"}"),
                "Blueprint factory must remain functional after the derived builder is mutated");
        assertDoesNotThrow(() -> mutatedFactory.getSchema("{\"type\":\"string\"}"),
                "Mutated factory must be functional with the new ObjectMapper");
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) can be used to create a fully functional factory")
    void testBuilderProducesUsableFactory() {
        // GIVEN: a builder derived from the blueprint
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // WHEN: we build a new factory from the derived builder
        JsonSchemaFactory newFactory = derivedBuilder.build();
        // THEN: the new factory must be able to resolve the default meta‑schema URI
        String schemaWithMeta = String.format("{\"$schema\":\"%s\",\"type\":\"string\"}", DEFAULT_META_SCHEMA_URI);
        JsonSchema schema = newFactory.getSchema(schemaWithMeta);
        assertNotNull(schema, "Schema created from the new factory must not be null");
        assertDoesNotThrow(() -> schema.validate(new ObjectMapper().readTree("\"hello\"")),
                "Validation of a valid instance must not throw");
    }

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

    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies added meta‑schemas")
    void testBuilderCopiesMetaSchemas() {
        SimpleMetaSchema meta1 = new SimpleMetaSchema("urn:meta:1");
        SimpleMetaSchema meta2 = new SimpleMetaSchema("urn:meta:2");
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

    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFactory mappings")
    void testBuilderCopiesUriFactoryMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .uriFactory(dummyUriFactory, CUSTOM_SCHEME)
                .build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFactory schemeFactory = getPrivateField(derivedFactory, "uriFactory");
        Map<String, URIFactory> map = getPrivateField(schemeFactory, "uriFactories");
        assertEquals(dummyUriFactory, map.get(CUSTOM_SCHEME));
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFetcher mappings")
    void testBuilderCopiesUriFetcherMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .uriFetcher(dummyUriFetcher, CUSTOM_SCHEME)
                .build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFetcher schemeFetcher = getPrivateField(derivedFactory, "uriFetcher");
        Map<String, URIFetcher> map = getPrivateField(schemeFetcher, "uriFetchers");
        assertEquals(dummyUriFetcher, map.get(CUSTOM_SCHEME));
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies URITranslators")
    void testBuilderCopiesUriTranslators() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .addUriTranslator(dummyUriTranslator)
                .addUriTranslator(dummyUriTranslator2)
                .build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> list = getPrivateField(translator, "translators");
        assertEquals(2, list.size());
        assertSame(dummyUriTranslator, list.get(0));
        assertSame(dummyUriTranslator2, list.get(1));
    }
}
```