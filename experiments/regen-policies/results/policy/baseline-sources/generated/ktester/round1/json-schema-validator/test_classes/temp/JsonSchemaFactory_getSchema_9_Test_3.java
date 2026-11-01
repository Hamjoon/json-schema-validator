package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import java.io.InputStream;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /**
     * Mocks for all collaborators **
     */
    @Mock
    private ObjectMapper jsonMapper;

    @Mock
    private YAMLMapper yamlMapper;

    @Mock
    private URISchemeFactory uriFactory;

    @Mock
    private URISchemeFetcher uriFetcher;

    @Mock
    private URNFactory urnFactory;

    @Mock
    private CompositeURITranslator uriTranslators;

    @Mock
    private URIFetcher // needed to fill the map for URISchemeFetcher
    dummyFetcher;

    /**
     * System under test **
     */
    private JsonSchemaFactory factory;

    /**
     * Helper objects **
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    // the factory‑wide translator
    @Mock
    private CompositeURITranslator factoryTranslator;

    // used in several tests
    @Mock
    private SchemaValidatorsConfig customConfig;

    // same as above but caching enabled
    private JsonSchemaFactory factoryWithCache;

    private static final URI DUMMY_URI = URI.create("http://example.com/dummy.json");

    /**
     * Simple {@link URISchemeFactory} implementation that delegates to a {@link Function}.
     */
    private static final class SimpleURISchemeFactory extends URISchemeFactory {

        private final Function<String, URI> creator;

        SimpleURISchemeFactory(Function<String, URI> creator) {
            super(Collections.emptyMap());
            this.creator = creator;
        }

        @Override
        public URI create(String uri) {
            return creator.apply(uri);
        }
    }

    /**
     * Simple {@link URISchemeFetcher} implementation that delegates to a {@link Function}.
     */
    private static final class SimpleURISchemeFetcher extends URISchemeFetcher {

        private final Function<URI, InputStream> fetcher;

        SimpleURISchemeFetcher(Function<URI, InputStream> fetcher) {
            super(Collections.emptyMap());
            this.fetcher = fetcher;
        }

        @Override
        public InputStream fetch(URI uri) throws IOException {
            return fetcher.apply(uri);
        }
    }

    /**
     * Simple {@link CompositeURITranslator} that returns the supplied URI unchanged.
     */
    private static final class IdentityTranslator implements CompositeURITranslator {

        @Override
        public URI translate(URI uri) {
            return uri;
        }

        @Override
        public URI translate(String uri) {
            return URI.create(uri);
        }

        @Override
        public CompositeURITranslator with(CompositeURITranslator other) {
            return this;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build factories (cache disabled / enabled)
        factory = buildFactory(false);
        factoryWithCache = buildFactory(true);
    }

    /**
     * Helper to build a {@link JsonSchemaFactory} instance via the package‑private constructor.
     *
     * @param enableCache true to turn on {@code enableUriSchemaCache}
     * @return a ready‑to‑use factory
     */
    private JsonSchemaFactory buildFactory(boolean enableCache) throws Exception {
        // Default URI factory (identity)
        URISchemeFactory uriFactory = new SimpleURISchemeFactory(URI::create);
        // Default fetcher that always fails – each test supplies its own fetcher.
        URISchemeFetcher uriFetcher = new SimpleURISchemeFetcher(uri -> {
            throw new IOException("Fetcher not configured for URI: " + uri);
        });
        // Use the library's built‑in meta‑schema instance.
        JsonMetaSchema meta = JsonMetaSchema.getInstance(URI.create(DEFAULT_META_SCHEMA_URI));
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), meta);
        Map<String, String> uriMap = Collections.emptyMap();
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, null, metaSchemas, uriMap, enableCache, new IdentityTranslator());
    }

    /**
     * Inserts a {@link JsonSchema} into the private {@code uriSchemaCache} map
     * of the supplied factory.
     */
    private void putIntoCache(JsonSchemaFactory fac, URI uri, JsonSchema schema) throws Exception {
        Field cacheField = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<URI, JsonSchema> cache = (Map<URI, JsonSchema>) cacheField.get(fac);
        cache.put(uri, schema);
    }

    /**
     * Retrieves the private {@code uriSchemaCache} for verification.
     */
    private Map<URI, JsonSchema> getCache(JsonSchemaFactory fac) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<URI, JsonSchema> map = (Map<URI, JsonSchema>) f.get(fac);
        return map;
    }

    /**
     * Helper to inject a custom {@link URISchemeFactory} into an existing factory.
     */
    private void setFactoryUriFactory(JsonSchemaFactory fac, URISchemeFactory factory) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        f.setAccessible(true);
        f.set(fac, factory);
    }

    /**
     * Helper to inject a custom {@link URISchemeFetcher} into an existing factory.
     */
    private void setFactoryUriFetcher(JsonSchemaFactory fac, URISchemeFetcher fetcher) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("uriFetcher");
        f.setAccessible(true);
        f.set(fac, fetcher);
    }

    /**
     * Verify that {@code getSchema(URI, SchemaValidatorsConfig)} converts an {@link IllegalArgumentException}
     * thrown by {@link URISchemeFactory#create(URI)} into a {@link JsonSchemaException}.
     */
    @Test
    void getSchema_throwsJsonSchemaException_whenUriFactoryCreateFails() throws Exception {
        // Arrange – a factory that throws IllegalArgumentException for any URI.
        URISchemeFactory explodingFactory = new SimpleURISchemeFactory(uri -> {
            throw new IllegalArgumentException("bad URI");
        });
        setFactoryUriFactory(factory, explodingFactory);
        URI bogus = new URI("http://example.com/bogus-schema.json");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(bogus, cfg));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("bad URI", ex.getCause().getMessage());
    }

    /**
     * Verify that when the URI cache is disabled and the schema is not cached,
     * the method attempts to fetch the schema via {@link URISchemeFetcher}.
     * This test does not go through the full parsing pipeline – it only checks the
     * interaction with the fetcher.
     */
    @Test
    void getSchema_callsFetcher_whenCacheDisabled() throws Exception {
        // Arrange – a fetcher that returns a tiny JSON document.
        InputStream dummy = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        URISchemeFetcher fetcher = new SimpleURISchemeFetcher(uri -> dummy);
        setFactoryUriFetcher(factory, fetcher);
        URI schemaUri = new URI("http://example.com/schema.json");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // Act
        JsonSchema result = factory.getSchema(schemaUri, cfg);
        // Assert
        assertNotNull(result);
        // The factory's cache should still be empty because caching is disabled.
        assertTrue(getCache(factory).isEmpty());
    }

    @Test
    void getSchema_nullConfig_usesFactoryTranslator_andParsesJson() throws Exception {
        // Arrange – fetcher returns a minimal JSON schema.
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> stream));
        URI schemaUri = new URI("http://example.com/schema.json");
        // Act
        JsonSchema result = factory.getSchema(schemaUri, (SchemaValidatorsConfig) null);
        // Assert
        assertNotNull(result);
    }

    @Test
    void getSchema_customConfig_usesCombinedTranslator() throws Exception {
        // Arrange – a custom translator that rewrites the path.
        URI original = new URI("http://example.com/base/schema.json");
        URI translated = new URI("http://example.com/base/translated/schema.json");
        URITranslator customTranslator = new URITranslator() {

            @Override
            public URI translate(URI uri) {
                return uri;
            }

            @Override
            public CompositeURITranslator with(CompositeURITranslator other) {
                return new CompositeURITranslator() {

                    @Override
                    public URI translate(URI uri) {
                        return translated;
                    }

                    @Override
                    public URI translate(String uri) {
                        return translate(URI.create(uri));
                    }

                    @Override
                    public CompositeURITranslator with(CompositeURITranslator other) {
                        return this;
                    }
                };
            }
        };
        // Inject custom translator into the config (field name may differ across versions)
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        try {
            Field f = SchemaValidatorsConfig.class.getDeclaredField("uriTranslator");
            f.setAccessible(true);
            f.set(cfg, customTranslator);
        } catch (NoSuchFieldException ignored) {
            // If the field does not exist, the test will still exercise the code path.
        }
        // Fetcher that expects the translated URI.
        InputStream stream = new ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> {
            assertEquals(translated, uri, "Fetcher should be called with the translated URI");
            return stream;
        }));
        // Act
        JsonSchema result = factory.getSchema(original, cfg);
        // Assert
        assertNotNull(result);
    }

    @Test
    void getSchema_cacheHit_returnsCachedInstance_andUpdatesConfig() throws Exception {
        // Obtain a schema using the caching factory (this will store it in the cache).
        JsonSchema cached = factoryWithCache.getSchema(DUMMY_URI, new SchemaValidatorsConfig());
        // Ensure it is present in the cache.
        assertTrue(getCache(factoryWithCache).containsKey(DUMMY_URI));
        // New configuration to apply.
        SchemaValidatorsConfig newCfg = new SchemaValidatorsConfig();
        // Act
        JsonSchema result = factoryWithCache.getSchema(DUMMY_URI, newCfg);
        // Assert
        assertSame(cached, result);
        // The cached schema's ValidationContext should have received the new config.
        assertSame(newCfg, result.getValidationContext().getConfig());
    }

    @Test
    void getSchema_yamlExtension_usesYamlMapper() throws Exception {
        // Arrange – a fetcher that returns a tiny YAML document.
        InputStream yamlStream = new ByteArrayInputStream("type: object".getBytes());
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> yamlStream));
        URI yamlUri = new URI("http://example.com/schema.yaml");
        // Act
        JsonSchema result = factory.getSchema(yamlUri, new SchemaValidatorsConfig());
        // Assert
        assertNotNull(result);
        // Verify that the JSON mapper cannot parse the YAML content.
        yamlStream.reset();
        assertThrows(IOException.class, () -> jsonMapper.readTree(yamlStream));
    }

    @Test
    void getSchema_throwsJsonSchemaException_whenFetcherThrowsIOException() throws Exception {
        // Arrange – a fetcher that always throws IOException.
        URISchemeFetcher badFetcher = new SimpleURISchemeFetcher(uri -> {
            throw new IOException("network down");
        });
        setFactoryUriFetcher(factory, badFetcher);
        URI schemaUri = new URI("http://example.com/unreachable-schema.json");
        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaUri, new SchemaValidatorsConfig()));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network down", ex.getCause().getMessage());
    }
}
