
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
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.InputStream;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.uri.URITranslator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

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
        factory = ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, metaSchemas, uriMap, // enableUriSchemaCache
        false, uriTranslators);
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
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMeta);
        Map<String, String> uriMap = Collections.emptyMap();
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, metaSchemas, uriMap, enableCache, factoryTranslator);
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
        java.lang.reflect.Field cacheField = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<URI, JsonSchema> cache = (Map<URI, JsonSchema>) cacheField.get(fac);
        return cache;
    }

    /**
     * Verify that {@code getSchema(URI, SchemaValidatorsConfig)} converts an {@link IllegalArgumentException}
     * thrown by {@link URISchemeFactory#create(URI)} into a {@link JsonSchemaException}.
     */
    @Test
    void getSchema_throwsJsonSchemaException_whenUriFactoryCreateFails() throws Exception {
        // Arrange
        URI bogusUri = new URI("http://example.com/bogus-schema.json");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // Simulate the factory throwing an IllegalArgumentException when translating the URI.
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad URI"));
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(bogusUri, config));
        // The cause of the JsonSchemaException should be the original IllegalArgumentException.
        assertTrue(thrown.getCause() instanceof IllegalArgumentException);
        assertEquals("bad URI", thrown.getCause().getMessage());
        // Verify that the logger was invoked (optional – we only verify interaction with the mock).
        verify(uriFactory, times(1)).create(anyString());
    }

    /**
     * Verify that when the URI cache is disabled and the schema is not cached,
     * the method attempts to fetch the schema via {@link URISchemeFetcher}.
     * This test does not go through the full parsing pipeline – it only checks the
     * interaction with the fetcher.
     */
    @Test
    void getSchema_callsFetcher_whenCacheDisabled() throws Exception {
        // Arrange
        URI schemaUri = new URI("http://example.com/schema.json");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // Mock the URI translation and creation to return the same URI.
        when(uriFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArgument(0)));
        // Provide a dummy InputStream that represents a minimal JSON schema.
        InputStream dummyStream = new java.io.ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        when(uriFetcher.fetch(any(URI.class))).thenReturn(dummyStream);
        // Mock ObjectMapper to return a dummy JsonNode.
        JsonNode dummyNode = mock(JsonNode.class);
        when(jsonMapper.readTree(dummyStream)).thenReturn(dummyNode);
        // Mock the private helper methods indirectly by stubbing the public behaviour they affect.
        // The factory will call findMetaSchemaForSchema – we let it return the dummy meta‑schema we
        // placed in the constructor map (already satisfied).
        // The idMatchesSourceUri check will return false by default (no $id in our dummy schema).
        // Act
        JsonSchema result = factory.getSchema(schemaUri, config);
        // Assert
        assertNotNull(result, "Resulting JsonSchema should not be null");
        verify(uriFetcher, times(1)).fetch(any(URI.class));
        verify(jsonMapper, times(1)).readTree(any(InputStream.class));
    }

    @Test
    void getSchema_nullConfig_usesFactoryTranslator_andParsesJson() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI schemaUri = new URI("http://example.com/schema.json");
        // uriFactory simply returns the same URI string that it receives
        when(uriFactory.create(anyString())).thenAnswer(i -> new URI(i.getArgument(0)));
        // fetcher returns a tiny JSON document
        InputStream stream = new java.io.ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        when(uriFetcher.fetch(any(URI.class))).thenReturn(stream);
        JsonNode node = mock(JsonNode.class);
        when(jsonMapper.readTree(stream)).thenReturn(node);
        // we do not care about meta‑schema resolution; the dummy meta‑schema is already in the map
        // idMatchesSourceUri will return false because our node has no "$id"
        // (the real implementation will simply return false)
        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        JsonSchema result = factory.getSchema(schemaUri, null);
        // --------------------------------------------------------------
        // Assert
        // --------------------------------------------------------------
        assertNotNull(result, "Result must be a JsonSchema instance");
        // verify that the factory's default translator was used (i.e. config == null)
        verify(uriFactory, times(1)).create(eq(schemaUri.toString()));
        verify(uriFetcher, times(1)).fetch(eq(schemaUri));
        verify(jsonMapper, times(1)).readTree(stream);
    }

    @Test
    void getSchema_customConfig_usesCombinedTranslator() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI original = new URI("http://example.com/base/schema.json");
        URI translated = new URI("http://example.com/base/translated/schema.json");
        // mock a custom translator that rewrites the path
        URITranslator customTranslator = mock(URITranslator.class);
        when(customConfig.getUriTranslator()).thenReturn(customTranslator);
        // `with` must return another translator that actually does the translate
        when(customTranslator.with(factoryTranslator)).thenReturn(uri -> translated);
        // the factory will receive the translated URI string
        when(uriFactory.create(translated.toString())).thenReturn(translated);
        InputStream stream = new java.io.ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        when(uriFetcher.fetch(translated)).thenReturn(stream);
        JsonNode node = mock(JsonNode.class);
        when(jsonMapper.readTree(stream)).thenReturn(node);
        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        JsonSchema result = factory.getSchema(original, customConfig);
        // --------------------------------------------------------------
        // Assert
        // --------------------------------------------------------------
        assertNotNull(result);
        // ensure the factory sent the *translated* URI to the uriFactory
        verify(uriFactory, times(1)).create(eq(translated.toString()));
        verify(uriFetcher, times(1)).fetch(eq(translated));
    }

    @Test
    void getSchema_cacheHit_returnsCachedInstance_andUpdatesConfig() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI schemaUri = new URI("http://example.com/cached-schema.json");
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();
        // create a mock JsonSchema that carries a mock ValidationContext
        JsonSchema cached = mock(JsonSchema.class);
        ValidationContext vc = mock(ValidationContext.class);
        when(cached.getValidationContext()).thenReturn(vc);
        // pre‑populate the cache
        putIntoCache(factoryWithCache, schemaUri, cached);
        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        JsonSchema result = factoryWithCache.getSchema(schemaUri, newConfig);
        // --------------------------------------------------------------
        // Assert
        // --------------------------------------------------------------
        assertSame(cached, result, "Factory must return the cached JsonSchema");
        // the cached schema's ValidationContext should receive the new config
        verify(vc, times(1)).setConfig(newConfig);
        // no fetcher interaction should have happened
        verifyNoInteractions(uriFetcher);
    }

    @Test
    void getSchema_yamlExtension_usesYamlMapper() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI yamlUri = new URI("http://example.com/schema.yaml");
        when(uriFactory.create(anyString())).thenAnswer(i -> new URI(i.getArgument(0)));
        InputStream stream = new java.io.ByteArrayInputStream("type: object".getBytes());
        when(uriFetcher.fetch(yamlUri)).thenReturn(stream);
        JsonNode yamlNode = mock(JsonNode.class);
        when(yamlMapper.readTree(stream)).thenReturn(yamlNode);
        // stub doCreate path by spying the factory and returning a dummy JsonSchema
        JsonSchemaFactory spy = spy(factory);
        JsonSchema dummySchema = mock(JsonSchema.class);
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
        // --------------------------------------------------------------
        // Act
        // --------------------------------------------------------------
        JsonSchema result = spy.getSchema(yamlUri, new SchemaValidatorsConfig());
        // --------------------------------------------------------------
        // Assert
        // --------------------------------------------------------------
        assertSame(dummySchema, result);
        verify(yamlMapper, times(1)).readTree(stream);
        verify(jsonMapper, never()).readTree(any());
    }

    @Test
    void getSchema_throwsJsonSchemaException_whenFetcherThrowsIOException() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI schemaUri = new URI("http://example.com/unreachable-schema.json");
        when(uriFactory.create(anyString())).thenReturn(schemaUri);
        when(uriFetcher.fetch(schemaUri)).thenThrow(new IOException("network down"));
        // --------------------------------------------------------------
        // Act & Assert
        // --------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaUri, new SchemaValidatorsConfig()));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network down", ex.getCause().getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:14: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:15: error: package org.mockito does not exist
import org.mockito.MockitoAnnotations;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:20: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:28: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:29: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:31: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
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
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:55: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:70: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:74: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:88: error: cannot find symbol
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
                                   ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:89: error: cannot find symbol
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        ^
  symbol:   method when(String)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:109: error: cannot find symbol
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
                                   ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:110: error: cannot find symbol
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        ^
  symbol:   method when(String)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:151: error: cannot find symbol
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad URI"));
                               ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:158: error: cannot find symbol
        verify(uriFactory, times(1)).create(anyString());
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:158: error: cannot find symbol
        verify(uriFactory, times(1)).create(anyString());
                                            ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:173: error: cannot find symbol
        when(uriFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArgument(0)));
                               ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:176: error: cannot find symbol
        when(uriFetcher.fetch(any(URI.class))).thenReturn(dummyStream);
                              ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:178: error: cannot find symbol
        JsonNode dummyNode = mock(JsonNode.class);
                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:179: error: cannot find symbol
        when(jsonMapper.readTree(dummyStream)).thenReturn(dummyNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:188: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(any(URI.class));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:188: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(any(URI.class));
                                           ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:189: error: cannot find symbol
        verify(jsonMapper, times(1)).readTree(any(InputStream.class));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:189: error: cannot find symbol
        verify(jsonMapper, times(1)).readTree(any(InputStream.class));
                                              ^
  symbol:   method any(Class<InputStream>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:199: error: cannot find symbol
        when(uriFactory.create(anyString())).thenAnswer(i -> new URI(i.getArgument(0)));
                               ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:202: error: cannot find symbol
        when(uriFetcher.fetch(any(URI.class))).thenReturn(stream);
                              ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:203: error: cannot find symbol
        JsonNode node = mock(JsonNode.class);
                        ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:204: error: cannot find symbol
        when(jsonMapper.readTree(stream)).thenReturn(node);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:211: error: reference to getSchema is ambiguous
        JsonSchema result = factory.getSchema(schemaUri, null);
                                   ^
  both method getSchema(URI,SchemaValidatorsConfig) in JsonSchemaFactory and method getSchema(URI,JsonNode) in JsonSchemaFactory match
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:217: error: cannot find symbol
        verify(uriFactory, times(1)).create(eq(schemaUri.toString()));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:217: error: cannot find symbol
        verify(uriFactory, times(1)).create(eq(schemaUri.toString()));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:218: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(eq(schemaUri));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:218: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(eq(schemaUri));
                                           ^
  symbol:   method eq(URI)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:219: error: cannot find symbol
        verify(jsonMapper, times(1)).readTree(stream);
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:230: error: cannot find symbol
        URITranslator customTranslator = mock(URITranslator.class);
                                         ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:231: error: cannot find symbol
        when(customConfig.getUriTranslator()).thenReturn(customTranslator);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:233: error: cannot find symbol
        when(customTranslator.with(factoryTranslator)).thenReturn(uri -> translated);
                             ^
  symbol:   method with(CompositeURITranslator)
  location: variable customTranslator of type URITranslator
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:235: error: cannot find symbol
        when(uriFactory.create(translated.toString())).thenReturn(translated);
        ^
  symbol:   method when(URI)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:237: error: cannot find symbol
        when(uriFetcher.fetch(translated)).thenReturn(stream);
        ^
  symbol:   method when(InputStream)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:238: error: cannot find symbol
        JsonNode node = mock(JsonNode.class);
                        ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:239: error: cannot find symbol
        when(jsonMapper.readTree(stream)).thenReturn(node);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:249: error: cannot find symbol
        verify(uriFactory, times(1)).create(eq(translated.toString()));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:249: error: cannot find symbol
        verify(uriFactory, times(1)).create(eq(translated.toString()));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:250: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(eq(translated));
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:250: error: cannot find symbol
        verify(uriFetcher, times(1)).fetch(eq(translated));
                                           ^
  symbol:   method eq(URI)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:261: error: cannot find symbol
        JsonSchema cached = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:262: error: cannot find symbol
        ValidationContext vc = mock(ValidationContext.class);
                               ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:263: error: cannot find symbol
        when(cached.getValidationContext()).thenReturn(vc);
        ^
  symbol:   method when(ValidationContext)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:275: error: cannot find symbol
        verify(vc, times(1)).setConfig(newConfig);
                   ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:277: error: cannot find symbol
        verifyNoInteractions(uriFetcher);
        ^
  symbol:   method verifyNoInteractions(URISchemeFetcher)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:286: error: cannot find symbol
        when(uriFactory.create(anyString())).thenAnswer(i -> new URI(i.getArgument(0)));
                               ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:288: error: cannot find symbol
        when(uriFetcher.fetch(yamlUri)).thenReturn(stream);
        ^
  symbol:   method when(InputStream)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:289: error: cannot find symbol
        JsonNode yamlNode = mock(JsonNode.class);
                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:290: error: cannot find symbol
        when(yamlMapper.readTree(stream)).thenReturn(yamlNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:292: error: cannot find symbol
        JsonSchemaFactory spy = spy(factory);
                                ^
  symbol:   method spy(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:293: error: cannot find symbol
        JsonSchema dummySchema = mock(JsonSchema.class);
                                 ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                 ^
  symbol:   method any()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                        ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                                     ^
  symbol:   method any()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                                            ^
  symbol:   method any()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                                                   ^
  symbol:   method any()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:294: error: cannot find symbol
        doReturn(dummySchema).when(spy).doCreate(any(), anyString(), any(), any(), any(), anyBoolean());
                                                                                          ^
  symbol:   method anyBoolean()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:303: error: cannot find symbol
        verify(yamlMapper, times(1)).readTree(stream);
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:304: error: cannot find symbol
        verify(jsonMapper, never()).readTree(any());
                           ^
  symbol:   method never()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:304: error: cannot find symbol
        verify(jsonMapper, never()).readTree(any());
                                             ^
  symbol:   method any()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:313: error: cannot find symbol
        when(uriFactory.create(anyString())).thenReturn(schemaUri);
                               ^
  symbol:   method anyString()
  location: class JsonSchemaFactory_getSchema_9_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_9_Test.java:314: error: cannot find symbol
        when(uriFetcher.fetch(schemaUri)).thenThrow(new IOException("network down"));
        ^
  symbol:   method when(InputStream)
  location: class JsonSchemaFactory_getSchema_9_Test
75 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tjava.util.concurrent.ConcurrentMap<java.net.URI, com.networknt.schema.JsonSchema> com.networknt.schema.JsonSchemaFactory.uriSchemaCache;\n\t\tboolean com.networknt.schema.JsonSchemaFactory.enableUriSchemaCache;\n\t\tcom.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\tcom.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;\n\t\torg.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;\n\t\tcom.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.SchemaValidatorsConfig`:\n\trelated functions:\n\t\tmethod `public void addKeywordWalkListener(JsonSchemaWalkListener)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


