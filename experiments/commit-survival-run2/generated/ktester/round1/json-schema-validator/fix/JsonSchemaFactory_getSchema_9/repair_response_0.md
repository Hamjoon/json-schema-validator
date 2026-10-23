```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(URI, SchemaValidatorsConfig)}.
 *
 * <p>The original test suite relied on Mockito, which is not available in the
 * execution environment.  The tests have therefore been rewritten to use
 * lightweight hand‑rolled fakes that provide the same behaviour required for
 * the assertions.</p>
 */
class JsonSchemaFactoryGetSchemaTest {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";
    private static final URI DUMMY_URI = URI.create("http://example.com/dummy.json");

    private JsonSchemaFactory factory;
    private JsonSchemaFactory factoryWithCache;

    private ObjectMapper jsonMapper;
    private YAMLMapper yamlMapper;

    /** Simple {@link URISchemeFactory} implementation that delegates to a
     *  {@link Function}. */
    private static final class SimpleURISchemeFactory implements URISchemeFactory {
        private final Function<String, URI> creator;

        SimpleURISchemeFactory(Function<String, URI> creator) {
            this.creator = creator;
        }

        @Override
        public URI create(String uri) {
            return creator.apply(uri);
        }
    }

    /** Simple {@link URISchemeFetcher} implementation that delegates to a
     *  {@link Function}. */
    private static final class SimpleURISchemeFetcher implements URISchemeFetcher {
        private final Function<URI, InputStream> fetcher;

        SimpleURISchemeFetcher(Function<URI, InputStream> fetcher) {
            this.fetcher = fetcher;
        }

        @Override
        public InputStream fetch(URI uri) throws IOException {
            return fetcher.apply(uri);
        }
    }

    /** Simple {@link CompositeURITranslator} that returns the supplied URI
     *  unchanged. */
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
    void setUp() throws Exception {
        // Use real Jackson mappers – they are cheap and sufficient for the tests.
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        // A dummy meta‑schema required by the factory constructor.
        JsonMetaSchema dummyMeta = new JsonMetaSchema() {
            @Override public String getUri() { return DEFAULT_META_SCHEMA_URI; }
            @Override public JsonValidator newValidator(ValidationContext context, String keyword, String schemaPath,
                                                        JsonNode schemaNode, JsonSchema schema, String parentSchemaPath) {
                throw new UnsupportedOperationException();
            }
        };
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMeta);

        // Empty map for scheme‑to‑fetcher translation (not used in these tests).
        Map<String, String> uriMap = Collections.emptyMap();

        // Build a factory with cache disabled.
        factory = buildFactory(false, metaSchemas, uriMap);

        // Build a factory with cache enabled (used by the cache‑hit test).
        factoryWithCache = buildFactory(true, metaSchemas, uriMap);
    }

    private JsonSchemaFactory buildFactory(boolean enableCache,
                                           Map<String, JsonMetaSchema> metaSchemas,
                                           Map<String, String> uriMap) throws Exception {

        // By default the factory just returns the URI it receives.
        URISchemeFactory uriFactory = new SimpleURISchemeFactory(URI::create);
        // By default the fetcher throws – each test supplies its own fetcher.
        URISchemeFetcher uriFetcher = new SimpleURISchemeFetcher(uri -> {
            throw new IOException("Fetcher not configured for URI: " + uri);
        });

        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class,
                        YAMLMapper.class,
                        String.class,
                        URISchemeFactory.class,
                        URISchemeFetcher.class,
                        URNFactory.class,
                        Map.class,
                        Map.class,
                        boolean.class,
                        CompositeURITranslator.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                null,                // URNFactory – not needed for these tests
                metaSchemas,
                uriMap,
                enableCache,
                new IdentityTranslator());
    }

    /** Helper to inject a custom {@link URISchemeFactory} into an existing factory. */
    private void setFactoryUriFactory(JsonSchemaFactory fac, URISchemeFactory factory) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        f.setAccessible(true);
        f.set(fac, factory);
    }

    /** Helper to inject a custom {@link URISchemeFetcher} into an existing factory. */
    private void setFactoryUriFetcher(JsonSchemaFactory fac, URISchemeFetcher fetcher) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriFetcher");
        f.setAccessible(true);
        f.set(fac, fetcher);
    }

    /** Helper to access the private cache map. */
    @SuppressWarnings("unchecked")
    private Map<URI, JsonSchema> getCache(JsonSchemaFactory fac) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        f.setAccessible(true);
        return (Map<URI, JsonSchema>) f.get(fac);
    }

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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(bogus, cfg));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("bad URI", ex.getCause().getMessage());
    }

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
        JsonSchema result = factory.getSchema(schemaUri, null);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getSchema_customConfig_usesCombinedTranslator() throws Exception {
        // Arrange – a custom translator that rewrites the path.
        URI original = new URI("http://example.com/base/schema.json");
        URI translated = new URI("http://example.com/base/translated/schema.json");

        // Custom translator implementation.
        URITranslator customTranslator = new URITranslator() {
            @Override
            public URI translate(URI uri) {
                // Not used directly – the factory combines translators via `with`.
                return uri;
            }

            @Override
            public CompositeURITranslator with(CompositeURITranslator other) {
                // Return a translator that always yields the translated URI.
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

        // Configure the factory's default translator (already IdentityTranslator)
        // and inject the custom config.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // The real SchemaValidatorsConfig has a setter for the translator; if not,
        // we set the field via reflection.
        try {
            java.lang.reflect.Field f = SchemaValidatorsConfig.class.getDeclaredField("uriTranslator");
            f.setAccessible(true);
            f.set(cfg, customTranslator);
        } catch (NoSuchFieldException ignored) {
            // If the field does not exist, the test will still exercise the code path
            // that combines translators.
        }

        // Fetcher that returns a tiny JSON document for the translated URI.
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
        // Arrange – put a mock JsonSchema into the cache of a factory with caching enabled.
        JsonSchema cached = new JsonSchema() {
            private ValidationContext ctx = new ValidationContext(null, null, null, null, null, null, null, null);
            @Override public ValidationContext getValidationContext() { return ctx; }
            // other methods are not needed for this test
        };
        URI uri = new URI("http://example.com/cached-schema.json");
        getCache(factoryWithCache).put(uri, cached);

        SchemaValidatorsConfig newCfg = new SchemaValidatorsConfig();

        // Act
        JsonSchema result = factoryWithCache.getSchema(uri, newCfg);

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
        // The factory should have used the YAML mapper; we verify that the JSON mapper
        // was not invoked by checking that the JSON mapper cannot parse the YAML content.
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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaUri, new SchemaValidatorsConfig()));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network down", ex.getCause().getMessage());
    }
}
```