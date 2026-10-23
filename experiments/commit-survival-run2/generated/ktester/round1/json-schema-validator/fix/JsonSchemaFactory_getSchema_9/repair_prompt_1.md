
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
import com.networknt.schema.uri.URITranslator;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.function.Function;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.HashMap;
import java.util.Collections;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;

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
     * Simple {@link URISchemeFactory} implementation that delegates to a
     *  {@link Function}.
     */
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

    /**
     * Simple {@link URISchemeFetcher} implementation that delegates to a
     *  {@link Function}.
     */
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

    /**
     * Simple {@link CompositeURITranslator} that returns the supplied URI
     *  unchanged.
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
        // Initialise the mocks (handled by MockitoExtension) and create a real instance
        // of JsonSchemaFactory via reflection because its constructor is package‑private.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        // The factory expects the default meta‑schema to be present in the map.
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMeta);
        Map<String, String> uriMap = Collections.emptyMap();
        // Build the JsonSchemaFactory instance using reflection.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = // enableUriSchemaCache
        ctor.// enableUriSchemaCache
        newInstance(// enableUriSchemaCache
        jsonMapper, // enableUriSchemaCache
        yamlMapper, // enableUriSchemaCache
        DEFAULT_META_SCHEMA_URI, // enableUriSchemaCache
        uriFactory, // enableUriSchemaCache
        uriFetcher, // enableUriSchemaCache
        urnFactory, // enableUriSchemaCache
        metaSchemas, // enableUriSchemaCache
        uriMap, false, uriTranslators);
        factory = buildFactory(false);
        factoryWithCache = buildFactory(true);
        // Use real Jackson mappers – they are cheap and sufficient for the tests.
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        // A dummy meta‑schema required by the factory constructor.
        JsonMetaSchema dummyMeta = new JsonMetaSchema() {

            @Override
            public String getUri() {
                return DEFAULT_META_SCHEMA_URI;
            }

            @Override
            public JsonValidator newValidator(ValidationContext context, String keyword, String schemaPath, JsonNode schemaNode, JsonSchema schema, String parentSchemaPath) {
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

    /**
     * Helper to build a {@link JsonSchemaFactory} instance via the package‑private constructor.
     *
     * @param enableCache true to turn on {@code enableUriSchemaCache}
     * @return a ready‑to‑use factory
     */
    private JsonSchemaFactory buildFactory(boolean enableCache) throws Exception {
        // By default the factory just returns the URI it receives.
        URISchemeFactory uriFactory = new SimpleURISchemeFactory(URI::create);
        // By default the fetcher throws – each test supplies its own fetcher.
        URISchemeFetcher uriFetcher = new SimpleURISchemeFetcher(uri -> {
            throw new IOException("Fetcher not configured for URI: " + uri);
        });
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, // URNFactory – not needed for these tests
        null, metaSchemas, uriMap, enableCache, new IdentityTranslator());
    }

    /**
     * Inserts a {@link JsonSchema} into the private {@code uriSchemaCache} map
     * of the supplied factory.
     */
    private void putIntoCache(JsonSchemaFactory fac, URI uri, JsonSchema schema) throws Exception {
        java.lang.reflect.Field cacheField = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<URI, JsonSchema> cache = (Map<URI, JsonSchema>) cacheField.get(fac);
        cache.put(uri, schema);
    }

    /**
     * Retrieves the private {@code uriSchemaCache} for verification.
     */
    private Map<URI, JsonSchema> getCache(JsonSchemaFactory fac) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        f.setAccessible(true);
        return (Map<URI, JsonSchema>) f.get(fac);
    }

    /**
     * Helper to inject a custom {@link URISchemeFactory} into an existing factory.
     */
    private void setFactoryUriFactory(JsonSchemaFactory fac, URISchemeFactory factory) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        f.setAccessible(true);
        f.set(fac, factory);
    }

    /**
     * Helper to inject a custom {@link URISchemeFetcher} into an existing factory.
     */
    private void setFactoryUriFetcher(JsonSchemaFactory fac, URISchemeFetcher fetcher) throws Exception {
        java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriFetcher");
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

            @Override
            public ValidationContext getValidationContext() {
                return ctx;
            }
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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaUri, new SchemaValidatorsConfig()));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network down", ex.getCause().getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:19: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:22: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:82: error: interface expected here
    private static final class SimpleURISchemeFactory implements URISchemeFactory {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:100: error: interface expected here
    private static final class SimpleURISchemeFetcher implements URISchemeFetcher {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:118: error: interface expected here
    private static final class IdentityTranslator implements CompositeURITranslator {
                                                             ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:27: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:42: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:45: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:48: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:51: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:66: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:70: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:90: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:108: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:120: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:125: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:130: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:132: error: incompatible types: IdentityTranslator cannot be converted to CompositeURITranslator
            return this;
                   ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:142: error: cannot find symbol
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
                                   ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:143: error: cannot find symbol
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        ^
  symbol:   method when(String)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:166: error: variable dummyMeta is already defined in method setupBeforeEach()
        JsonMetaSchema dummyMeta = new JsonMetaSchema() {
                       ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:166: error: constructor JsonMetaSchema in class JsonMetaSchema cannot be applied to given types;
        JsonMetaSchema dummyMeta = new JsonMetaSchema() {
                                   ^
  required: String,String,Map<String,Keyword>
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:178: error: variable metaSchemas is already defined in method setupBeforeEach()
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
                                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:181: error: variable uriMap is already defined in method setupBeforeEach()
        Map<String, String> uriMap = Collections.emptyMap();
                            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:183: error: method buildFactory in class JsonSchemaFactory_getSchema_9_Test cannot be applied to given types;
        factory = buildFactory(false, metaSchemas, uriMap);
                  ^
  required: boolean
  found:    boolean,Map<String,JsonMetaSchema>,Map<String,String>
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:185: error: method buildFactory in class JsonSchemaFactory_getSchema_9_Test cannot be applied to given types;
        factoryWithCache = buildFactory(true, metaSchemas, uriMap);
                           ^
  required: boolean
  found:    boolean,Map<String,JsonMetaSchema>,Map<String,String>
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:196: error: incompatible types: SimpleURISchemeFactory cannot be converted to URISchemeFactory
        URISchemeFactory uriFactory = new SimpleURISchemeFactory(URI::create);
                                      ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:198: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        URISchemeFetcher uriFetcher = new SimpleURISchemeFetcher(uri -> {
                                      ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:204: error: cannot find symbol
        null, metaSchemas, uriMap, enableCache, new IdentityTranslator());
              ^
  symbol:   variable metaSchemas
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:204: error: cannot find symbol
        null, metaSchemas, uriMap, enableCache, new IdentityTranslator());
                           ^
  symbol:   variable uriMap
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:253: error: incompatible types: SimpleURISchemeFactory cannot be converted to URISchemeFactory
        URISchemeFactory explodingFactory = new SimpleURISchemeFactory(uri -> {
                                            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:275: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        URISchemeFetcher fetcher = new SimpleURISchemeFetcher(uri -> dummy);
                                   ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:291: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> stream));
                                      ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: reference to getSchema is ambiguous
        JsonSchema result = factory.getSchema(schemaUri, null);
                                   ^
  both method getSchema(URI,SchemaValidatorsConfig) in JsonSchemaFactory and method getSchema(URI,JsonNode) in JsonSchemaFactory match
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:313: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:323: error: method does not override or implement a method from a supertype
                    @Override
                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:328: error: method does not override or implement a method from a supertype
                    @Override
                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:350: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> {
                                      ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:363: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema cached = new JsonSchema() {
                            ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:365: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
            private ValidationContext ctx = new ValidationContext(null, null, null, null, null, null, null, null);
                                            ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    <null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:387: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        setFactoryUriFetcher(factory, new SimpleURISchemeFetcher(uri -> yamlStream));
                                      ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:401: error: incompatible types: SimpleURISchemeFetcher cannot be converted to URISchemeFetcher
        URISchemeFetcher badFetcher = new SimpleURISchemeFetcher(uri -> {
                                      ^
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
45 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tjava.util.concurrent.ConcurrentMap<java.net.URI, com.networknt.schema.JsonSchema> com.networknt.schema.JsonSchemaFactory.uriSchemaCache;\n\t\tboolean com.networknt.schema.JsonSchemaFactory.enableUriSchemaCache;\n\t\tcom.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\tcom.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;\n\t\torg.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;\n\t\tcom.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.SchemaValidatorsConfig`:\n\trelated functions:\n\t\tmethod `public void addKeywordWalkListener(JsonSchemaWalkListener)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


