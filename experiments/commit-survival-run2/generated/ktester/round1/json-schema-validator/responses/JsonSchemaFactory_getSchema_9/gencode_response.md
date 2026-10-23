```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal operation with a null config (default URI translator)</li>
 *   <li>custom {@link SchemaValidatorsConfig} providing its own {@link URITranslator}</li>
 *   <li>exception handling when {@link URISchemeFactory#create(String)} throws</li>
 *   <li>cache‑hit behaviour when caching is enabled</li>
 *   <li>YAML detection (yamlMapper is used)</li>
 *   <li>IO‑related failure paths (fetcher returns null, close() throws, etc.)</li>
 *   <li>handling of a null {@code schemaUri}</li>
 * </ul>
 *
 * Private helper methods are accessed via reflection where necessary.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /*** Mocks for all collaborators ***/
    @Mock private ObjectMapper jsonMapper;
    @Mock private YAMLMapper yamlMapper;
    @Mock private URISchemeFactory uriFactory;
    @Mock private URISchemeFetcher uriFetcher;
    @Mock private URNFactory urnFactory;
    @Mock private CompositeURITranslator factoryTranslator; // the factory‑wide translator
    @Mock private SchemaValidatorsConfig customConfig;      // used in several tests

    /*** System under test ***/
    private JsonSchemaFactory factory;          // created per‑test via reflection
    private JsonSchemaFactory factoryWithCache; // same as above but caching enabled

    /*** Constants ***/
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";
    private static final URI DUMMY_URI = URI.create("http://example.com/dummy.json");

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

        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class,
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
        return ctor.newInstance(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                urnFactory,
                metaSchemas,
                uriMap,
                enableCache,
                factoryTranslator);
    }

    @BeforeEach
    void setUp() throws Exception {
        factory = buildFactory(false);
        factoryWithCache = buildFactory(true);
    }

    /* ---------------------------------------------------------------------- */
    /* Helper utilities */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /* 1. Null Config – default URI translator */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /* 2. Custom Config – custom URI translator */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /* 3. Invalid URI – IllegalArgumentException from URISchemeFactory */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_throwsJsonSchemaException_whenUriFactoryCreateFails() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI bogus = new URI("invalid://uri");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad URI"));

        // --------------------------------------------------------------
        // Act & Assert
        // --------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(bogus, cfg));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("bad URI", ex.getCause().getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Cache Hit – enableUriSchemaCache = true */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /* 5. Cache Miss – YAML schema detection */
    /* ---------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------- */
    /* 6. IOException while reading schema (fetcher returns stream that throws) */
    /* ---------------------------------------------------------------------- */

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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaUri, new SchemaValidatorsConfig()));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network down", ex.getCause().getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /* 7. InputStream closed in finally block (successful path) */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_closesInputStreamExactlyOnce_onSuccess() throws Exception {
        // --------------------------------------------------------------
        // Arrange
        // --------------------------------------------------------------
        URI schemaUri = new URI("http://example.com/valid-schema.json");
        when(uriFactory.create(anyString())).thenReturn(schemaUri);
        InputStream mockStream = mock(InputStream.class);
        when(uriFetcher.fetch(schemaUri)).thenReturn(mockStream);
        JsonNode node = mock(JsonNode.class);
        when(jsonMapper.readTree(mockStream)).thenReturn(node