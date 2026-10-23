
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
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import com.networknt.schema.uri.URITranslator;
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
     * Simple {@link URISchemeFactory} implementation that delegates to a {@link Function}.
     */
    private static final class SimpleURISchemeFactory extends URISchemeFactory {

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
     * Simple {@link URISchemeFetcher} implementation that delegates to a {@link Function}.
     */
    private static final class SimpleURISchemeFetcher extends URISchemeFetcher {

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
     * Simple {@link CompositeURITranslator} that returns the supplied URI unchanged.
     */
    private static final class IdentityTranslator extends CompositeURITranslator {

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
        JsonMetaSchema meta = JsonMetaSchema.getInstance(DEFAULT_META_SCHEMA_URI);
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), meta);
        Map<String, String> uriMap = Collections.emptyMap();
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
        // Arrange – put a mock JsonSchema into the cache of a factory with caching enabled.
        JsonSchema cached = new JsonSchema(new ValidationContext(null, null, null, null, null, null, null, null), null, null) {
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:22: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:24: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:28: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:46: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:49: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:52: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:67: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:71: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:86: error: constructor URISchemeFactory in class URISchemeFactory cannot be applied to given types;
        SimpleURISchemeFactory(Function<String, URI> creator) {
                                                              ^
  required: Map<String,URIFactory>
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:103: error: constructor URISchemeFetcher in class URISchemeFetcher cannot be applied to given types;
        SimpleURISchemeFetcher(Function<URI, InputStream> fetcher) {
                                                                   ^
  required: Map<String,URIFetcher>
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:123: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:128: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:155: error: cannot find symbol
        JsonMetaSchema meta = JsonMetaSchema.getInstance(DEFAULT_META_SCHEMA_URI);
                                            ^
  symbol:   method getInstance(String)
  location: class JsonMetaSchema
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:271: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:280: error: method does not override or implement a method from a supertype
                    @Override
                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:285: error: method does not override or implement a method from a supertype
                    @Override
                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:316: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        JsonSchema cached = new JsonSchema(new ValidationContext(null, null, null, null, null, null, null, null), null, null) {
                                           ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    <null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>
  reason: actual and formal argument lists differ in length
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
21 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tjava.util.concurrent.ConcurrentMap<java.net.URI, com.networknt.schema.JsonSchema> com.networknt.schema.JsonSchemaFactory.uriSchemaCache;\n\t\tboolean com.networknt.schema.JsonSchemaFactory.enableUriSchemaCache;\n\t\tcom.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\tcom.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;\n\t\torg.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;\n\t\tcom.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.SchemaValidatorsConfig`:\n\trelated functions:\n\t\tmethod `public void addKeywordWalkListener(JsonSchemaWalkListener)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


