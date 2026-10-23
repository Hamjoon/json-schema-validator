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
